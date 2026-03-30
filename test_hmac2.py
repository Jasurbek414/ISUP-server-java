import hashlib
import hmac

def hmac_sha256(key, data):
    return hmac.new(key, data, hashlib.sha256).hexdigest().upper()

def hmac_sha1(key, data):
    return hmac.new(key, data, hashlib.sha1).hexdigest().upper()

def md5(data):
    return hashlib.md5(data).digest()

# the devHmac from the log
target = "1B969047051DE8932F307905D39339471C7FE5D1C5D83495A2145105F066220D"
# the FULL 33-byte nonce from the log
nonce_hex = "301B969047051DE8932F307905D39339471C7FE5D1C5D83495A2145105F066220D"
nonce_bytes = bytes.fromhex(nonce_hex)
nonce1_bytes = bytes.fromhex(nonce_hex[:2])

passwords = ["isup1234", "12345", "123456", "admin123", "admin12345"]

for pwd in passwords:
    pwd_bytes = pwd.encode()
    pwd_md5 = md5(pwd_bytes)
    
    variants = [
        ("V1_nonce1", hmac_sha256(pwd_md5, nonce1_bytes)),
        ("V2_nonce1", hmac_sha256(pwd_bytes, nonce1_bytes)),
        ("V1_nonce33", hmac_sha256(pwd_md5, nonce_bytes)),
        ("V2_nonce33", hmac_sha256(pwd_bytes, nonce_bytes)),
    ]
    
    for name, result in variants:
        if result in target:
            print(f"MATCH FOUND! {name} with password '{pwd}' -> {result}")

print("Done testing.")
