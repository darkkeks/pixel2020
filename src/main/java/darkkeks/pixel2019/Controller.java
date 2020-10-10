package darkkeks.pixel2019;

import darkkeks.pixel2020.*;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.util.Set;
import java.util.concurrent.*;

import static darkkeks.pixel2020.PixelApiKt.FIELD_HEIGHT;
import static darkkeks.pixel2020.PixelApiKt.FIELD_WIDTH;

public class Controller {

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private final HttpClient httpClient;
    private final PixelQueue queue;
    private Template template;
    private BotHandler observer;

    private final BoardGraphics graphics;

    private final Set<BotHandler> accounts;
    private final ScheduledThreadPoolExecutor executor;

    private final HealthCheck healthCheck;
    private int lastMinuteStats;

    private final BlockingQueue<Integer> speedQueue;
    
    private static boolean needQueueRebuild = false;

    public Controller(Credentials observerCredentials, Template template) {
        this.template = template;
        this.accounts = ConcurrentHashMap.newKeySet();

        this.speedQueue = new ArrayBlockingQueue<>(128);

        executor = new ScheduledThreadPoolExecutor(24);
        graphics = new BoardGraphics(new BufferedImage(FIELD_WIDTH, FIELD_HEIGHT, BufferedImage.TYPE_INT_RGB));
        graphics.updateTemplate(template);
        httpClient = HttpClient.newHttpClient();

        healthCheck = new HealthCheck();
        queue = new PixelQueue(template);

        startTicker();

        addAccount(observerCredentials).thenAccept(account -> {
            observer = account;
            hookObserver();
        });

        runBot();
    }

    private void runBot() {
        executor.scheduleAtFixedRate(() -> {
            String output = String.format("Accounts active: %5d, queue size: %5d", accounts.size(), queue.size());
            
            if (needQueueRebuild) {
                queue.rebuild(graphics.getImage());
            }

            speedQueue.offer(queue.size());
            if(speedQueue.size() > 60) {
                int prev = speedQueue.poll();
                output += String.format(", current speed: %5.2f pixels/second", (prev - queue.size()) / 60d);
            }

            logger.info(output);

            try {
                accounts.forEach(account -> {
                    if (account.getCanPlace()) {
                        if (queue.size() > 0) {
                            PixelQueue.Point point = queue.pop();
                            Color color = template.getColorAbs(point.getX(), point.getY());
                            logger.info("Placing pixel x=" + point.getX() + ", y=" + point.getY());
                            Pixel pixel = new Pixel(point.getX(), point.getY(), color);
                            CompletableFuture.supplyAsync(() -> {
                                healthCheck.onPlace(pixel, account.getCredentials());
                                try {
                                    return account.sendPixel(pixel).get();
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }, executor).thenRun(() -> {
                                executor.schedule(() -> {
                                    if (healthCheck.checkHealth(pixel)) {
                                        lastMinuteStats++;
                                    }
                                }, 20, TimeUnit.SECONDS);
                            });
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Exception sending pixel", e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(() -> {
            logger.info("Rebuilding queue");
            queue.rebuild(graphics.getImage());
        }, 0, 30, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(() -> {
            logger.info("Placed during last minute: " + lastMinuteStats);
            lastMinuteStats = 0;
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void startTicker() {
        executor.scheduleAtFixedRate(() -> {
            accounts.forEach(BotHandler::tick);
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void reconnect(BotHandler account) {
        accounts.remove(account);
        executor.schedule(() -> {
            addAccount(account.getCredentials()).thenAccept(newAccount -> {
                if (account == observer) {
                    observer = newAccount;
                    hookObserver();
                }
            });
        }, 5, TimeUnit.SECONDS);
    }

    public CompletableFuture<BotHandler> addAccount(Credentials credentials) {
        BotHandler account = new BotHandler(credentials, httpClient);
        account.setCloseHandler(() -> {
            this.reconnect(account);
            return Unit.INSTANCE;
        });
        return account.connect().thenApply(v -> {
            accounts.add(account);
            return account;
        });
    }

    private void hookObserver() {
        observer.getPixelApi().data().thenAccept(board -> {
            graphics.updateBoard(board);
            queue.rebuild(board);
        });

        observer.setPixelHandler(pixel -> {
            healthCheck.onPixel(pixel);
            queue.onPixelChange(pixel);
            if (graphics != null) {
                graphics.setPixel(pixel.getX(), pixel.getY(), pixel.getColor());
            }
            return Unit.INSTANCE;
        });
    }
    
    public void updateTemplate(Template newTemplate) {
        template = newTemplate;
        graphics.updateTemplate(template);
        requestQueueRebuild();
    }
    
    public static void requestQueueRebuild() {
        Controller.needQueueRebuild = true;
    }
}
