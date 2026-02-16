package signupAutomation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;

import java.time.Duration;
import java.util.List;
import emailAutomation.TempEmail;

public class SignupTest {
    public static void main(String[] args) {

        TempEmail tempEmail = new TempEmail();
        
        String testEmail = null;
        int attempts = 0;
        while (testEmail == null && attempts < 10) {
            testEmail = tempEmail.getEmailAddress();
            if (testEmail == null || testEmail.isEmpty()) {
                System.out.println("Waiting for email generation...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                attempts++;
            }
        }
        
        System.out.println("Using temporary email: " + testEmail);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        driver.manage().window().maximize();
        driver.get("https://authorized-partner.vercel.app/");

        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Get Started')]"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@role='checkbox']"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue')]"))).click();

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Enter Your First Name']"))).sendKeys("Rama");
            driver.findElement(By.xpath("//input[@placeholder='Enter Your Last Name']")).sendKeys("Limbu");
            driver.findElement(By.xpath("//input[@placeholder='Enter Your Email Address']")).sendKeys(testEmail);

            selectCountry(driver, wait);
            
 ////////////////////Ph number send
 /// //Need to change number each time
            WebElement phoneField = wait.until(ExpectedConditions.elementToBeClickable(By.name("phoneNumber")));
            phoneField.clear();
            phoneField.sendKeys("9812228983");

            //password send
            WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(By.name("password")));
            passwordField.sendKeys("Rama@123");

            //Confirm password
            WebElement confirmPasswordField = wait.until(ExpectedConditions.elementToBeClickable(By.name("confirmPassword")));
            confirmPasswordField.sendKeys("Rama@123");

            //click next button
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Next')]")));
            
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
            Thread.sleep(500);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);

            System.out.println("Form submitted! Now checking temp email for OTP...");
            Thread.sleep(5000);

            System.out.println("Fetching OTP from temporary email");
            String otpCode = tempEmail.fetchOTP(60);
            
            if (otpCode == null) {
                System.out.println("Failed to fetch OTP automatically.");
                return;
            }
            
            System.out.println("OTP fetched automatically: " + otpCode);

            // Find OTP input fields
            System.out.println("Looking for OTP input fields...");
            
            // Wait for OTP fields to be visible
            Thread.sleep(2000);
            
            List<WebElement> otpFields = driver.findElements(By.cssSelector("input[data-input-otp='true']"));
            
            System.out.println("Found " + otpFields.size() + " OTP input boxes");
            
            // Handle different OTP input scenarios
            if (otpFields.size() >= 6) {
                // Multiple separate input boxes (6 individual fields)
                System.out.println("Detected multiple OTP input boxes - entering one digit per box");
                for (int i = 0; i < otpCode.length() && i < otpFields.size(); i++) {
                    WebElement field = otpFields.get(i);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", field);
                    Thread.sleep(100);
                    field.click();
                    Thread.sleep(100);
                    field.clear();
                    field.sendKeys(String.valueOf(otpCode.charAt(i)));
                    System.out.println("Entered digit " + (i+1) + ": " + otpCode.charAt(i));
                    Thread.sleep(300);
                }
            } else if (otpFields.size() == 1) {
                // enter all digits at once
                System.out.println("Detected single OTP input field - entering all digits at once");
                WebElement otpField = otpFields.get(0);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", otpField);
                Thread.sleep(500);
                
                // Click to focus
                otpField.click();
                Thread.sleep(300);
                
                otpField.clear();
                Thread.sleep(200);
                
                // Try multiple methods to enter the OTP
                System.out.println("Method 1: Using sendKeys");
                otpField.sendKeys(otpCode);
                Thread.sleep(1000);
                
                // Check if value was entered
                String enteredValue = otpField.getAttribute("value");
                System.out.println("Value in field after sendKeys: '" + enteredValue + "'");
                
                if (enteredValue == null || enteredValue.isEmpty() || !enteredValue.equals(otpCode)) {
                    System.out.println("Method 2: Using JavaScript to set value");
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                        otpField, otpCode
                    );
                    Thread.sleep(500);
                    
                    // Trigger change event
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                        otpField
                    );
                    Thread.sleep(500);
                }
                
                // Character-by-character entry as fallback
                enteredValue = otpField.getAttribute("value");
                if (enteredValue == null || enteredValue.isEmpty() || !enteredValue.equals(otpCode)) {
                    System.out.println("Method 3: Entering character by character");
                    otpField.clear();
                    Thread.sleep(200);
                    otpField.click();
                    Thread.sleep(200);
                    
                    for (char digit : otpCode.toCharArray()) {
                        otpField.sendKeys(String.valueOf(digit));
                        Thread.sleep(150);
                    }
                }
                
                System.out.println("OTP entered: " + otpCode);
            } else {
                System.out.println("No OTP fields found! Trying alternative selectors...");
                
                // Try to find any input field on the page
                List<WebElement> allInputs = driver.findElements(By.tagName("input"));
                System.out.println("Found " + allInputs.size() + " total input elements");
                
                for (WebElement input : allInputs) {
                    String type = input.getAttribute("type");
                    String inputMode = input.getAttribute("inputmode");
                    String autoComplete = input.getAttribute("autocomplete");
                    
                    if ("text".equals(type) || "tel".equals(type) || "numeric".equals(inputMode) || 
                        "one-time-code".equals(autoComplete)) {
                        System.out.println("Found potential OTP field - attempting to enter code");
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", input);
                        Thread.sleep(300);
                        input.click();
                        Thread.sleep(200);
                        input.clear();
                        input.sendKeys(otpCode);
                        Thread.sleep(500);
                        System.out.println("Entered OTP in alternative field");
                        break;
                    }
                }
            }
            
            System.out.println("OTP entry attempt completed!");
            Thread.sleep(2000);

            // Click Verify button
            try {
                WebElement verifyButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Verify Code')]")
                ));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", verifyButton);
                System.out.println("Verify button clicked!");
                Thread.sleep(3000);
                
                // Check if verification was successful by looking for error messages
                List<WebElement> errorMessages = driver.findElements(By.xpath("//*[contains(text(),'Please Enter Valid Code')]"));
                if (errorMessages.size() > 0) {
                    System.out.println("⚠️ Verification failed - 'Please Enter Valid Code' message is still showing");
                    return;
                } else {
                    System.out.println("✅ OTP verification completed successfully!");
                }
                
            } catch (Exception e) {
                System.out.println("Verify button clicked automatically or not needed: " + e.getMessage());
            }

            //---Agency wala part----
            //------------------------------------//
            
            System.out.println("\n Waiting for Agency page to load ===");
            Thread.sleep(5000);
            
            // Wait for page to load
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Enter Agency Name']")
            ));
            System.out.println("Agency page loaded!");
            
            //Agency name
            WebElement agencyNameField = driver.findElement(By.xpath("//input[@placeholder='Enter Agency Name']"));
            agencyNameField.clear();
            agencyNameField.sendKeys("Tech Solution Agency");
            System.out.println("✓ Entered Agency Name");
            Thread.sleep(500);
            
            //Role fill
            WebElement roleField = driver.findElement(By.xpath("//input[@placeholder='Enter Your Role in Agency']"));
            roleField.clear();
            roleField.sendKeys("CEO");
            System.out.println("✓ Entered Role");
            Thread.sleep(500);
            
            //Agency Email
            WebElement agencyEmail = driver.findElement(By.xpath("//input[@placeholder='Enter Your Agency Email Address']"));
            agencyEmail.clear();
            agencyEmail.sendKeys("email@gmail.com");
            System.out.println("✓ Entered Agency Email");
            Thread.sleep(500);
            
            //Website fill ...using multiple selectors to find it
            WebElement websiteField = null;
            try {
                websiteField = driver.findElement(By.xpath("//input[@placeholder='https:// Enter Your Agency Website']"));
            } catch (Exception e1) {
                {
                    try {
                        websiteField = driver.findElement(By.xpath("//input[contains(@placeholder,'Agency Website')]"));
                    } catch (Exception e2) {
                        System.out.println(" Could not find website field with any selector");
                    }
                }
            }
            
            if (websiteField != null) {
                websiteField.clear();
                websiteField.sendKeys("www.techsolution.com");
                System.out.println(" Entered Website");
            } else {
                System.out.println(" Not found");
            }
            Thread.sleep(500);
            
            //Fill Address
            WebElement fillAddress = driver.findElement(By.xpath("//input[@placeholder='Enter Your Agency Address']"));
            fillAddress.clear();
            fillAddress.sendKeys("Kathmandu, Nepal");
            System.out.println(" Entered Address");
            Thread.sleep(500);
           
            
            //Region ko lagi

            // Click dropdown
            driver.findElement(By.xpath("//button[@role='combobox']")).click();
            Thread.sleep(1000);
//type aus
            WebElement searchBox = driver.findElement(By.xpath("//input[@placeholder='Search...']"));
            searchBox.sendKeys("Australia");
            Thread.sleep(2000);

         // Click on Australia from the results
            
            
            driver.findElement(By.xpath("//span[contains(text(),'Australia')]")).click();
           
            Thread.sleep(1000);
            
            //Click Next
            try {
                WebElement agencyNextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Next')]")
                ));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", agencyNextBtn);
                Thread.sleep(500);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", agencyNextBtn);
                System.out.println("✓ Clicked Next - Agency form submitted!");
                Thread.sleep(6000);
                
                
                //------Professional experience wala part-----
                //Years of experience
                WebElement experienceDropdown =wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@role='combobox']")));
                experienceDropdown.click();
                Thread.sleep(500);
                
                //click 1 year
                driver.findElement(By.xpath("(//div[@role='option'])[1]")).click();
                
              Thread.sleep(500);
              
              //No.of student part
              WebElement studentsCount = driver.findElement(By.xpath("//input[@placeholder='Enter an approximate number.']"));
              studentsCount.clear();
              studentsCount.sendKeys("100");
              System.out.println("Entered Students count");
              Thread.sleep(500);
             
                //Focus Area
              WebElement focusArea = driver.findElement(By.xpath("//input[@placeholder='E.g., Undergraduate admissions to Canada.']"));
              focusArea.clear();
              focusArea.sendKeys("Web Development");
              	Thread.sleep(500);
              	
              	//Success Metrics
              	 WebElement successMetrics = driver.findElement(By.xpath("//input[contains(@placeholder, 'E.g., 90%')]"));
              	successMetrics.clear();
              	successMetrics.sendKeys("80%");
              	Thread.sleep(500);
              	
              	//Service Provided
              	driver.findElement(By.xpath("//label[contains(text(),'Career Counseling')]")).click();
              	Thread.sleep(300);
              	driver.findElement(By.xpath("//label[contains(text(),'Admission Applications')]")).click();
              	Thread.sleep(300);
              	driver.findElement(By.xpath("//label[contains(text(),'Visa Processing')]")).click();
              	Thread.sleep(2000);
              
                //Click NExt
              	driver.findElement(By.xpath("//button[contains(text(),'Next')]")).click();
              	Thread.sleep(3000);
              	
              	//Verification and preferences
              	//Business registration Number
              	WebElement businessRegNum = driver.findElement(By.xpath("//input[@placeholder='Enter your registration number']"));
              	businessRegNum.clear();
              	businessRegNum.sendKeys("123456789");
              	Thread.sleep(500);
              	
              	//Preferred countries
             // Preferred Countries dropdown
             
              	System.out.println("Selecting Preferred Countries...");

              	// Click the dropdown
              	driver.findElement(By.xpath("//button[@role='combobox']")).click();
              	Thread.sleep(2000);

              	// Type "Australia" in search
              	WebElement psearchBox = driver.findElement(By.xpath("//input[@placeholder='Search...']"));
              	psearchBox.sendKeys("Australia");
              	Thread.sleep(2000);

              	// Click Australia
              	WebElement australia = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[text()='Australia']")));
                    australia.click();
              	Thread.sleep(1500);

              	// Clear search and type France
              	psearchBox.clear();
              	psearchBox.sendKeys("France");
              	Thread.sleep(2000);

              	// Click France
            	WebElement France = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[text()='France']")));
                    France.click();
              	Thread.sleep(1500);
              	
              	//Baira click to close dropdown
              	driver.findElement(By.tagName("body")).click();
              	Thread.sleep(1000);
              	
              	//Prefered institution types
              	driver.findElement(By.xpath("//*[text()='Universities']")).click();

			  	driver.findElement(By.xpath("//*[text()='Other']")).click();
			  	Thread.sleep(500);
              	
              	
              	//Certification Details
              	WebElement certDetails = driver.findElement(By.xpath("//input[@placeholder='E.g., ICEF Certified Education Agent']"));
              	certDetails.clear();
              	certDetails.sendKeys("Broadways certified in qa and pm");
              	Thread.sleep(1000);

              	System.out.println("✓ Selected Australia and Canada");
              	
              	
              	
              	//Upload file
              	 driver.findElement(By.xpath("//input[@type='file']"))
                 .sendKeys("C:\\Users\\HomePC\\Desktop\\file.pdf");
           
           System.out.println("File uploaded!");
              	
           
           //Submit bttn
           driver.findElement(By.xpath("//button[text()='Submit']")).click();
              	
              	
                System.out.println("\n Form submitted successfully");
            } catch (Exception e) {
                System.out.println("Could not submit form: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\nPress any key in console to close browsers...");
            try {
                System.in.read();
            } catch (Exception e) {
                e.printStackTrace();
            }
            driver.quit();
            tempEmail.closeEmailBrowser();
        }
    }

    private static void selectCountry(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement dropdownButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@role='combobox']")));
        dropdownButton.click();
        Thread.sleep(500);

        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@placeholder='Search...']")));
        searchBox.clear();
        searchBox.sendKeys("Nepal");
        Thread.sleep(1000);

        WebElement nepalOption = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//div[contains(text(),'+977')] | //div[contains(text(),'NP')]")
        ));
        
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nepalOption);
        Thread.sleep(300);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nepalOption);
        Thread.sleep(1000);
        
        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.ESCAPE).perform();
        Thread.sleep(500);
    }
}