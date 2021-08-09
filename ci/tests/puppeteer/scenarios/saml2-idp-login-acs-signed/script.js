const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');
const cas = require('../../cas.js');
const assert = require("assert");

function cleanUp(exec) {
    exec.kill();
    let metadataDir = path.join(__dirname, '/saml-md');
    fs.rmdir(metadataDir, {recursive: true}, () => {
    });
    metadataDir = path.join(__dirname, '/saml-sp');
    fs.rmdir(metadataDir, {recursive: true}, () => {
    });
}

(async () => {
    let samlSpDir = path.join(__dirname, '/saml-sp');
    let idpMetadataPath = path.join(__dirname, '/saml-md/idp-metadata.xml');
    let exec = await cas.launchSamlSp(idpMetadataPath, samlSpDir, ['-DacsUrl=https://httpbin.org/post', '-DsignAuthnRequests=true']);
    await cas.waitFor('https://localhost:9876/sp/saml/status', async function () {
        const browser = await puppeteer.launch(cas.browserOptions());
        const page = await cas.newPage(browser);

        console.log("Trying without an exising SSO session...")
        page.goto("https://localhost:9876/sp")
        await page.waitForTimeout(3000)
        await page.waitForSelector('#idpForm', {visible: true});
        await cas.submitForm(page, "#idpForm");
        await page.waitForTimeout(2000)
        await page.waitForSelector('#username', {visible: true});
        await cas.loginWith(page, "casuser", "Mellon");
        await page.waitForResponse(response => response.status() === 200)
        await page.waitForTimeout(3000)
        console.log("Page URL: " + page.url());
        await page.waitForSelector('body pre', { visible: true });
        let content = await cas.textContent(page, "body pre");
        let payload = JSON.parse(content);
        console.log(payload);
        assert(payload.form.SAMLResponse !== null);
        
        console.log("Trying with an exising SSO session...")
        await page.goto("https://localhost:8443/cas/logout");
        await page.goto("https://localhost:8443/cas/login");
        await cas.loginWith(page, "casuser", "Mellon");
        await cas.assertTicketGrantingCookie(page);
        page.goto("https://localhost:9876/sp")
        await page.waitForTimeout(2000)
        await page.waitForSelector('#idpForm', {visible: true});
        await cas.submitForm(page, "#idpForm");
        await page.waitForTimeout(2000)
        console.log("Page URL: " + page.url());
        await page.waitForSelector('body pre', { visible: true });
        content = await cas.textContent(page, "body pre");
        payload = JSON.parse(content);
        console.log(payload);
        assert(payload.form.SAMLResponse !== null);

        await browser.close();
        console.log("Killing SAML2 SP process...");
        cleanUp(exec);
    }, async function (error) {
        cleanUp(exec);
        console.log(error);
        throw error;
    })
})();

