import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import darkkeks.pixel2020.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class PixelAccount {

    private static final Logger logger = LoggerFactory.getLogger(PixelAccount.class);

    private final PixelApi pixelApi;
    private final Credentials loginSignature;
    private String wsUrl;
    private String dataUrl;
    private WebsocketClient websocketClient;

    private int ttl;
    private int wait;

    private Consumer<Pixel> pixelConsumer;
    private Consumer<PixelAccount> onClose;

    public PixelAccount(Credentials loginSignature, HttpClient client, Consumer<PixelAccount> onClose) {
        this.loginSignature = loginSignature;
        this.onClose = onClose;
        pixelApi = new PixelApi(loginSignature.getSignature(), client);
    }

    public CompletableFuture<Void> start() {
        return pixelApi.start().thenAccept(data -> {
            wsUrl = data.get("url").getAsString();
            dataUrl = data.get("data").getAsString();
            //TODO deadline ?
            connectWebSocket();
        });
    }

    private void connectWebSocket() {
        websocketClient = new WebsocketClient(wsUrl + loginSignature.getSignature(), this);
    }

    public void tick() {
        if (wait > 0) wait--;
    }

    public boolean canPlace() {
        return wait == 0;
    }

    public Future<Void> sendPixel(Pixel pixel) {
        if (!canPlace()) throw new RuntimeException("Wait > 0: " + wait);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(PixelKt.pack(pixel));
        buffer.flip();

        wait += ttl + 1;
        return websocketClient.sendBinary(buffer);
    }

    public void handleMessage(String message) {
//        System.out.println(message);
        if(message.equals("DOUBLE_CONNECT")) {
            System.out.println("Double connect. Reconnecting.");
            websocketClient.close();
            return;
        }
        if(message.equals("restart")) {
            System.out.println("Server asked for a restart :)");
            websocketClient.close();
            return;
        }

        handleMessage(JsonParser.parseString(message).getAsJsonObject());
    }

    private void handleMessage(JsonObject message) {
        logger.info("Handling mesasge {}", message);

        JsonElement value = message.get("v");
        int type = message.get("t").getAsInt();

        switch (type) {
            case 2:
                JsonObject result = value.getAsJsonObject();
                if(result.has("wait")) {
                    wait = result.get("wait").getAsInt();
                }
                ttl = result.get("ttl").getAsInt();
                break;
            case 3:
                logger.info("Server asked for a restart :)");
                websocketClient.close();
                break;
            case 8:
                break;
            case 12:
                value.getAsJsonArray().forEach(item -> handleMessage(item.getAsJsonObject()));
            default:
                logger.warn("Unknown message: " + message);
        }
    }

    public void handleBinaryMessage(byte[] b) {
        ByteBuffer buffer = ByteBuffer.wrap(b);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i < b.length / 3 / 4; ++i) {
            Pixel pixel = PixelKt.unpack(buffer.getInt(), buffer.getInt(), buffer.getInt());
            if (pixelConsumer != null) {
                pixelConsumer.accept(pixel);
            }
        }
    }

    public void close() {
        onClose.accept(this);
    }

    public PixelApi getPixelApi() {
        return pixelApi;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public Credentials getLoginSignature() {
        return loginSignature;
    }

    public void setPixelConsumer(Consumer<Pixel> pixelConsumer) {
        this.pixelConsumer = pixelConsumer;
    }
}
