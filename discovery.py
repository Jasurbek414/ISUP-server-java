import requests
from requests.auth import HTTPDigestAuth

IP = "192.168.1.19"
USER = "admin"
PASS = "Password2026"

def final_discovery():
    paths = [
        "/ISAPI/Network/Advanced/PlatformAccess/ISUP?format=json",
        "/ISAPI/Network/Advanced/PlatformAccess/Ehome?format=json",
        "/ISAPI/Network/Advanced/Ehome?format=json",
        "/ISAPI/EHome/channels/1",
        "/ISAPI/Ehome/capabilities",
        "/ISAPI/Network/Advanced/PlatformAccess/ehome?format=json",
        "/ISAPI/Network/Advanced/PlatformAccess/ISUP",
        "/ISAPI/Network/Advanced/PlatformAccess/Ehome",
        "/ISAPI/Network/Advanced/Ehome",
        "/ISAPI/External/HCP/Ehome/channels/1"
    ]
    
    for path in paths:
        url = f"http://{IP}{path}"
        try:
            resp = requests.get(url, auth=HTTPDigestAuth(USER, PASS), timeout=3)
            print(f"Path: {path} | Status: {resp.status_code}")
            if resp.status_code == 200:
                print(f"BINGO! Path Found: {path}")
                print(f"Data: {resp.text}")
                return path
        except:
            pass
    return None

if __name__ == "__main__":
    path = final_discovery()
    if path:
        print(f"Configuration endpoint identified at {path}")
