import hashlib, hmac as hmac_lib

nonce1 = bytes.fromhex("DB")
dev_id = "DSK1T343EWX".encode()
target = "DAA9B254CE84B823A049FB897FB606074F4D7E311373433D84D82C2C780BDCC5"

passwords = ["isup1234", "test1234", "Password2025", "A1234567a", "Hik12345", "admin12345", "123456", "a1234567a", "admin", "12345678", "abc12345", "a12345678", "P@ssword2025", "Hik123456"]

for pwd in passwords:
    key_md5 = hashlib.md5(pwd.encode()).digest()
    
    # Try all prefixes/suffixes
    h1 = hmac_lib.new(key_md5, nonce1, hashlib.sha256).hexdigest()
    if h1.lower() == target.lower(): print(f"MATCH: {pwd} hmac_md5_nonce1")

    h2 = hmac_lib.new(pwd.encode(), nonce1, hashlib.sha256).hexdigest()
    if h2.lower() == target.lower(): print(f"MATCH: {pwd} hmac_pwd_nonce1")

    # Variant 3: HMAC(MD5(pwd), nonce1 + devId)
    h3 = hmac_lib.new(key_md5, nonce1 + dev_id, hashlib.sha256).hexdigest()
    if h3.lower() == target.lower(): print(f"MATCH: {pwd} hmac_md5_nonce1_devid")
    
    # Variant 3b: HMAC(MD5(pwd), devId + nonce1)
    h3b = hmac_lib.new(key_md5, dev_id + nonce1, hashlib.sha256).hexdigest()
    if h3b.lower() == target.lower(): print(f"MATCH: {pwd} hmac_md5_devid_nonce1")

    # Double MD5?
    key_md5_2 = hashlib.md5(key_md5).digest()
    h6 = hmac_lib.new(key_md5_2, nonce1, hashlib.sha256).hexdigest()
    if h6.lower() == target.lower(): print(f"MATCH: {pwd} hmac_md5_md5_nonce1")
