import requests
import time
import sys


def get_token(username, password):
    url = f"https://oauth.vk.com/token?grant_type=password&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&username={username}&password={password}"

    response = requests.get(url, allow_redirects=True)
    try:
        return response.json()['access_token']
    except:
        print(response.json())
        return None


def get_url(token):
    url = f"https://api.vk.com/method/execute.getServiceApp?v=5.97&" \
          f"app_id=7148888&" \
          f"access_token={token}"

    response = requests.get(url, allow_redirects=True)
    print(response.json())
    try:
        return response.json()['response']['app']['view_url']
    except:
        print(response.json())
        return None


token = get_token(sys.argv[1], sys.argv[2])

print(token)
print(get_url(token))

exit(0)

accs = [
]

for account in accs:
    account = account.split(':')
    print(account)
    token = get_token(*account)
    if token is not None:
        url = get_url(token)
        print(url)
    print()
    time.sleep(2)
