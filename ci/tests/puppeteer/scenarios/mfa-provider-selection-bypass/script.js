const puppeteer = require("puppeteer");
const cas = require("../../cas.js");

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    
    await page.goto("https://localhost:8443/cas/login?service=https://google.com");
    await cas.loginWith(page, "casuser", "Mellon");
    await page.waitForTimeout(1000);
    console.log("Selecting mfa-gauth");
    await cas.assertInvisibility(page, '#mfa-gauth');
    await cas.assertInvisibility(page, '#mfa-yubikey');
    await cas.assertTicketParameter(page);
    await page.goto("https://localhost:8443/cas/login");
    await cas.assertTicketGrantingCookie(page);
    
    await browser.close();
})();
