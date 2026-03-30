import hashlib
import hmac

def hmac_sha256(key, data):
    return hmac.new(key, data, hashlib.sha256).hexdigest().upper()

def hmac_sha1(key, data):
    return hmac.new(key, data, hashlib.sha1).hexdigest().upper()

def md5(data):
    return hashlib.md5(data).digest()

target = "28D11C25FEADE25D3116A405263BF2EE2B5C95338BF11DE0102BBF4243D59250"
passwords = ["isup1234", "Password2025", "12345"]
# The first byte was 95 (hex).
nonce_bytes = bytes.fromhex("95")

for pwd in passwords:
    pwd_bytes = pwd.encode()
    pwd_md5 = md5(pwd_bytes)
    
    # Try variants
    variants = [
        ("V1", hmac_sha256(pwd_md5, nonce_bytes)),
        ("V2", hmac_sha256(pwd_bytes, nonce_bytes)),
        ("V5", hmac_sha1(pwd_md5, nonce_bytes)),
        ("V6", hmac_sha1(pwd_bytes, nonce_bytes))
    ]
    
    for name, result in variants:
        if result in target:
            print(f"MATCH FOUND! {name} with password '{pwd}' -> {result}")
        else:
            print(f"No match: {name}({pwd}) = {result[:16]}...")
