import requests
from requests.auth import HTTPDigestAuth
import xml.etree.ElementTree as ET

IP = "192.168.1.19"
USER = "admin"
PASS = "Password2026"

def advanced_setup():
    # 1. Search for existing XML
    paths = [
        "/ISAPI/Network/Advanced/PlatformAccess/Ehome",
        "/ISAPI/Network/Advanced/HCPlatform",
        "/ISAPI/Network/Advanced/AlarmCenter"
    ]
    
    for path in paths:
        url = f"http://{IP}{path}"
        try:
            get_resp = requests.get(url, auth=HTTPDigestAuth(USER, PASS), timeout=5)
            if get_resp.status_code == 200:
                print(f"SUCCESS! Found config at {path}")
                xml_str = get_resp.text
                print(f"Original XML: {xml_str}")
                
                # 2. Modify XML for ISUP
                if "PlatformAccess" in path or "Ehome" in path:
                    xml_str = xml_str.replace("<enabled>false</enabled>", "<enabled>true</enabled>")
                    xml_str = xml_str.replace("<addressingFormatType>ipAddress</addressingFormatType>", "<addressingFormatType>hostname</addressingFormatType>")
                    xml_str = xml_str.replace("<ipAddress></ipAddress>", "<ipAddress>fake-faceid.uzinc.uz</ipAddress>")
                    xml_str = xml_str.replace("<hostName></hostName>", "<hostName>fake-faceid.uzinc.uz</hostName>")
                    xml_str = xml_str.replace("<portNo>7660</portNo>", "<portNo>17660</portNo>")
                    xml_str = xml_str.replace("<password></password>", "<password>isup1234</password>")
                    xml_str = xml_str.replace("<isEnableEncryption>true</isEnableEncryption>", "<isEnableEncryption>false</isEnableEncryption>")
                
                # 3. PUT it back
                put_resp = requests.put(url, auth=HTTPDigestAuth(USER, PASS), data=xml_str, timeout=5)
                print(f"PUT Status ({path}): {put_resp.status_code}")
                if put_resp.status_code == 200:
                    print("CONFIGURATION APPLIED.")
        except Exception as e:
            print(f"Error on {path}: {e}")

if __name__ == "__main__":
    advanced_setup()
