### Initial Setup

Checkout:

```agsl
git clone ...
```

Create local config file:
```agsl
cat .rig42krc
--index-gdoc-id 1X-GODC-ID
--dest-dir      C:\\User\\Temp\\YourSite
--site-base-url https://example.com/YourSite/
```

Run once to start the process of generating the Google Drive OAuth:

- https://code.google.com/apis/console/?api=drive
- (nav > API > Credentials > ID > Download)
- Save to `C:\Users\$USER\.rig42k\client_secret.json`
- Run again... GDrive shows OAuth. App has not been verified thus "continue / allow"
