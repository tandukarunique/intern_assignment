package emailAutomation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempEmail {
    private WebDriver emailDriver;
    private String emailAddress;

    public TempEmail() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-position=1000,0");
        
        this.emailDriver = new ChromeDriver(options);
        generateTempEmail();
    }

    private void generateTempEmail() {
        try {
            emailDriver.get("https://www.guerrillamail.com/");
            Thread.sleep(5000);

            JavascriptExecutor js = (JavascriptExecutor) emailDriver;
            this.emailAddress = (String) js.executeScript("return document.getElementById('email-widget').innerText;");
            System.out.println("Generated temporary email: " + emailAddress);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public String fetchOTP(int timeoutSeconds) {
        System.out.println("Waiting for OTP email...");
        long startTime = System.currentTimeMillis();
        JavascriptExecutor js = (JavascriptExecutor) emailDriver;
        
        int lastEmailCount = 0;

        while ((System.currentTimeMillis() - startTime) < (timeoutSeconds * 1000L)) {
            try {
                Thread.sleep(3000);

                // Remove ads
                js.executeScript("document.querySelectorAll('iframe[id*=\"aswift\"]').forEach(f => f.remove());");
                
                // Count emails
                Long emailCount = (Long) js.executeScript("return document.querySelectorAll('#email_list tr').length;");
                int currentCount = emailCount.intValue();
                
                if (currentCount > lastEmailCount) {
                    System.out.println("New email detected! Opening...");
                    lastEmailCount = currentCount;
                    
                    // Click the email
                    js.executeScript("document.querySelectorAll('#email_list tr')[0].click();");
                    Thread.sleep(4000);
                    
                    
                    try {
                        WebElement emailFrame = emailDriver.findElement(By.id("emailFrame"));
                        emailDriver.switchTo().frame(emailFrame);
                        System.out.println("Switched to email iframe");
                        
                        // Now get the content from inside the iframe
                        String emailContent = emailDriver.findElement(By.tagName("body")).getText();
                        
                        // Switch back to main content
                        emailDriver.switchTo().defaultContent();
                        
                        System.out.println("Email content: " + emailContent);
                        
                        String otp = extractOTPFromText(emailContent);
                        if (otp != null) {
                            System.out.println("✅ OTP FOUND: " + otp);
                            return otp;
                        }
                    } catch (Exception e) {
                        // If iframe doesn't exist, try direct access
                        emailDriver.switchTo().defaultContent();
                        
                        String bodyText = (String) js.executeScript(
                            "var b = document.getElementById('email_body');" +
                            "return b ? b.innerText : document.body.innerText;"
                        );
                        
                        System.out.println("Email text (no iframe): " + bodyText.substring(0, Math.min(500, bodyText.length())));
                        
                        String otp = extractOTPFromText(bodyText);
                        if (otp != null) {
                            System.out.println("✅ OTP FOUND: " + otp);
                            return otp;
                        }
                    }
                }

                System.out.println("Checking (" + currentCount + " emails)");

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        return null;
    }

    private String extractOTPFromText(String text) {
        if (text == null) return null;
        
        Pattern p = Pattern.compile("\\b(\\d{6})\\b");
        Matcher m = p.matcher(text);
        
        while (m.find()) {
            String code = m.group(1);
            if (!code.startsWith("233") && !code.startsWith("202") && !code.startsWith("144") && !code.startsWith("000")) {
                return code;
            }
        }
        
        return null;
    }

    public void closeEmailBrowser() {
        if (emailDriver != null) {
            emailDriver.quit();
        }
    }
}