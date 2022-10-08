package org.example;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.util.List;
import java.util.Scanner;

import static java.nio.file.Paths.*;
import static org.example.generateRandom.rng;
import static org.example.generateRandom.rsg;

public class App {
    static WebDriver driver;
    static boolean generateRandom = false;

    public static void main(String[] args) throws InterruptedException {
        String folderPath;
        Scanner sc = new Scanner(System.in);
        if(args.length != 0) {
            folderPath = args[0];
            if(args.length == 2) {
                generateRandom = args[1].equals("yes");
            }
        } else {
            System.out.print("Enter absolute path to CSVs folder: ");
            folderPath = sc.nextLine();
            System.out.print("To enable random values type \"yes\" without quotes: ");
            generateRandom = sc.next().equals("yes");
            sc.close();
        }
        String ctFile = get(folderPath, "ContractingTemplates.csv").toString();
        String feeFolder = get(folderPath, "Fees").toString();

        List<String[]> ctInpus = getDataFromFile(ctFile);
        if(ctInpus == null) {return;}

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://d5p05ae16v0dx.cloudfront.net/contractingtemplates/contractdetails");
        waitForSPLoader();
        String res = checkForErrorToast();
        if(!res.equals("")) {
            System.out.println(res);
            return;
        }

        CSVWriter ctOutputs = getCSVWriterFromPath(ctFile);
        if(ctOutputs == null) {return;}
        ctOutputs.writeNext(ctInpus.get(0));

        int ctNumber = 0;
//        int counter = 0;

        for(int i = 1; i < ctInpus.size(); i++) {
            String[] ctInputs = ctInpus.get(i);
            ctNumber++;
            String result[] = ctWrapper(ctInputs);
            fileWrite(ctOutputs, ctInputs, result);
            String feeFile = get(feeFolder, "Fees-"+ctNumber+".csv").toString();
            List<String[]> feeFileContents = getDataFromFile(feeFile);
            if(feeFileContents != null) {
                CSVWriter feeOutputs = getCSVWriterFromPath(feeFile);
                feeOutputs.writeNext(feeFileContents.get(0));
                String feeURL = driver.getCurrentUrl();
                for(int j = 1; j < feeFileContents.size(); j++) {
                    String[] feeInputs = feeFileContents.get(j);
                    String result1[] = {feeInputs[feeInputs.length-2], feeInputs[feeInputs.length-1]};
//                    if(!feeInputs[feeInputs.length-2].equals("passed") /*&& counter < 5*/) {
                        result1 = feeWrapper(feeInputs);
                        if(j != feeFileContents.size()-1) getBackToFees(feeURL);
//                        counter++;
//                    }
                    fileWrite(feeOutputs, feeInputs, result1);
                }
                closeCSVWriter(feeOutputs);
            }
            if(i != ctInpus.size()-1) getBackToCT();
        }
        closeCSVWriter(ctOutputs);
        driver.close();
    }

    private static void executeNoAlertOnReload() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.onbeforeunload = function() {};");
    }

    private static void fileWrite(CSVWriter writer, String[] values, String[] result) {
        values[values.length-2] = result[0];
        values[values.length-1] = result[1];
        writer.writeNext(values);
    }

    private static String[] ctWrapper(String[] ctInputs) {
        try {
            return createCT(ctInputs);
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"run unsuccessful", "internal error"};
        }
    }

    private static String[] feeWrapper(String[] feeInputs) {
        try {
            return createFees(feeInputs);
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"run unsuccessful", "internal error"};
        }
    }

    private static List<String[]> getDataFromFile(String filePath) {
        FileReader fileReader;
        try {
            fileReader = new FileReader(filePath);
        } catch (FileNotFoundException err) {
            System.out.println(filePath+" not found");
            return null;
        }
        CSVReader csvReader = new CSVReader(fileReader);
        List<String[]> output;
        try {
            output = csvReader.readAll();
            csvReader.close();
        } catch (Exception err) {
            System.out.println("CSV format invalid for "+filePath);
            return null;
        }
        try {
            fileReader.close();
            csvReader.close();
        } catch (Exception err) {
            return output;
        }
        return output;
    }

    private static CSVWriter getCSVWriterFromPath(String filePath) {
        FileWriter outputFile = null;
        try {
            outputFile = new FileWriter(filePath);
        } catch (Exception err) {
            System.out.println("Error creating writer object for "+filePath);
        }
        return new CSVWriter(outputFile, ',', '\0', '\0', "\n");
    }

    private static void closeCSVWriter(CSVWriter csvWriter) {
        try {
            csvWriter.close();
        } catch (Exception e) {
            System.out.println("E");
        }
    }

    private static String[] createCT(String[] ctInput) throws InterruptedException {
        String ce = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[2]/div[2]/div/div";
        String ae = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[3]/div[2]/div/div";
        String pc = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[4]/div[2]/div/div";
        String name = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[6]/div[2]/input";
        String grp = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[2]/div[4]/div/div";
        String isCustomCheckbox = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[3]/div[4]/span/span[1]/input";
        String isCustomInput = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[3]/div[4]/input";
        String isCustomDD = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[3]/div[4]/div/div";
        String dp = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[4]/div[4]/input";
        String ms = "/html/body/div[1]/div/div[3]/div/div[2]/div/div[2]/form/div[5]/div[4]/div/div";
        String snc = "//*[@id=\"SaveContCT\"]";

        String result;

        result = selectFromDropdown(ce, ctInput[0]);
        if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Contracting Entity Dropdown":"")};
        result = selectFromDropdown(ae, ctInput[1]);
        if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Acquiring Entity Dropdown":"")};
        result = selectFromDropdown(pc, ctInput[2]);
        if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Product Channel Dropdown":"")};
        waitForSPLoader();
        result = fillInputField(name, ctInput[3]+(generateRandom? rng():""));
        if(!result.equals("")) return new String[]{"failed", result};

        result = selectFromDropdown(grp, ctInput[4]);
        if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Grouping Dropdown":"")};

        if(ctInput[4].equals("Product Specific")) {
            result = selectFromDropdown(isCustomDD, ctInput[5]);
            if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Custom Dropdown":"")};
        } else if(!ctInput[5].equals("")) {
            result = checkCheckbox(isCustomCheckbox);
            if(!result.equals("")) return new String[]{"failed", result};
            result = fillInputField(isCustomInput, ctInput[5]);
            if(!result.equals("")) return new String[]{"failed", result};
        } else {
            driver.findElement(By.xpath(dp)).clear();
            result = fillInputField(dp, ctInput[6]+(generateRandom? rsg():""));
            if(!result.equals("")) return new String[]{"failed", result};
            result = selectFromDropdown(ms, ctInput[7]);
            if(!result.equals("")) return new String[]{"failed", result + (result.endsWith("found ")?"in Market Segment Dropdown":"")};

        }
        driver.findElement(By.xpath(snc)).click();
        Thread.sleep(2000);
        result = checkForErrorToast();
        if(!result.equals("")) return new String[]{"failed", result};
        waitForSPLoader();
        waitForSPLoader();
        return new String[]{"passed", ""};
    }

    public static void waitForSPLoader() throws InterruptedException {
        Thread.sleep(1000);
        while(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div")).isDisplayed()) {}
        Thread.sleep(1000);
    }

    private static String checkForErrorToast() {
        try {
            String output = driver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div/div/div[1]")).getText();
            if(output.toLowerCase().contains("success")) return "";
            return output;
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    public static String selectFromDropdown(String xpath, String value) throws InterruptedException {
        if(value.equals("")) {return "";}
        driver.findElement(By.xpath(xpath)).click();
        Thread.sleep(200);
        WebElement dropdown = driver.findElement(By.xpath("/html/body/div[3]/div[3]/ul"));
        List<WebElement> DDoptions = dropdown.findElements(By.tagName("li"));
        int i;
        for(i = 0; i < DDoptions.size(); i++) {
            if(DDoptions.get(i).getText().equals(value)) {
                DDoptions.get(i).click();
                break;
            }
        }
        Thread.sleep(1000);
        String res = checkForErrorToast();
        return !res.equals("")? res : i == DDoptions.size()? value+" not found ": "";
    }

    private static String fillInputField(String xpath, String value) throws InterruptedException {
        if(value.equals("")) {return "";}
        driver.findElement(By.xpath(xpath)).sendKeys(value);
        Thread.sleep(200);
        return checkForErrorToast();
    }

    private static String checkCheckbox(String xpath) throws InterruptedException {
        driver.findElement(By.xpath(xpath)).click();
        Thread.sleep(200);
        return checkForErrorToast();
    }

    public static String[] createFees(String[] feeInput) throws InterruptedException {
        String errorToast;
        errorToast = checkForErrorToast();
        if(!errorToast.equals("")) return new String[]{"failed", errorToast};
        int[] nav;
        int feeFormType;
        try {
            nav = getArray(feeInput[0]);
        } catch (NumberFormatException err) {
            return new String[]{"failed", "Invalid navigation format"};
        }
        boolean isFinal = feeInput[1].equals("Yes");
        try {
            feeFormType = Integer.parseInt(feeInput[2]);
            if(feeFormType != 1 && feeFormType != 2 && feeFormType != 3) {
                return new String[]{"failed", "Invalid fee form type"};
            }
        } catch (NumberFormatException err) {
            return new String[]{"failed", "Invalid fee form type format"};
        }

        String output[] = {};
        try {
            selectFees(nav, isFinal);
        } catch (NoSuchElementException err) {
            return new String[]{"failed", "Invalid navigation path"};
        }
        errorToast = checkForErrorToast();
        if(!errorToast.equals("")) return new String[]{"failed", errorToast};
        Thread.sleep(10000);
        if(feeFormType == 1) {
            String[] inputs = {feeInput[3], feeInput[4], feeInput[5], feeInput[6], feeInput[7], feeInput[8], feeInput[9], feeInput[10], feeInput[11], feeInput[12], feeInput[13], feeInput[14], feeInput[15], feeInput[16], feeInput[17], feeInput[18]};
            output = feeForm1Filler(inputs);
        } else if(feeFormType == 2) {
            String[] inputs = {feeInput[3], feeInput[4], feeInput[19], feeInput[13], feeInput[14], feeInput[16]};
            output = feeForm2Filler(inputs);
        } else if(feeFormType == 3) {
            String[] inputs = {feeInput[3], feeInput[4], feeInput[19], feeInput[14], feeInput[16]};
            output = feeForm3Filler(inputs);
        }
        Thread.sleep(1000);
        if(output.length != 0 && !output[1].equals("")) return output;
        driver.findElement(By.xpath("/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/button")).click();
        waitForSPLoader();
        errorToast = checkForErrorToast();
        if(!errorToast.equals("")) return new String[]{"failed", errorToast};
        Thread.sleep(2000);
        return output;
    }

    private static int[] getArray(String s) {
        String[] arr = s.split("/");
        int[] output = new int[arr.length];
        for(int i = 0; i < arr.length; i++) {
            output[i] = Integer.parseInt(arr[i]);
        }
        return output;
    }

    private static void selectFees(int[] nav, Boolean isFinal) throws InterruptedException {
        String xpath = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[1]/div/div/div";
        String dropdown = "/span/button";
        String button = "/span/span";
        String additive = "/ol/li";

        for(int i = 0; i < nav.length; i++) {
            String path;

            if(i != nav.length - 1 ) {
                path = xpath + additive + "[" +nav[i] + "]" + dropdown;
            } else {
                if(isFinal) {
                    button = button + "[2]";
                }
                path = xpath + additive + "[" + nav[i] + "]" + button;
            }

            driver.findElement(By.xpath(path)).click();
            xpath = xpath + additive;
            waitForSPLoader();
        }
        driver.findElement(By.xpath("/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[1]/div[2]")).click();
    }

    private static String[] feeForm1Filler(String[] input) throws InterruptedException {
        String pc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[2]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String pf = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[3]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String tc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[5]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String tt = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[6]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String cc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[7]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String tl = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[8]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String tt1 = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[10]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String mcc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[11]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String st = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[12]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String co = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[13]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String et = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[14]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String fc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[17]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String per = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div[1]/div/div/div[2]/div/div/div/div/input";
        String base = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div[2]/div/div[2]/div/div/div/div/input";
        String min = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div[3]/div/div/div[2]/div/div/div/div/input";
        String max = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div[4]/div/div/div[2]/div/div/div/div/input";

        String result;

        result = reactDropdownSelector(pc, 168, input[0]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Currency Dropdown":"")};

        result = reactDropdownSelector(pf, 9, input[1]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Frequency Dropdown":"")};

        result = reactDropdownSelector(tc, 168, input[2]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Transaction Currency Dropdown":"")};

        result = reactDropdownSelector(tt, 6, input[3]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Transaction Type Dropdown":"")};

        result = reactDropdownSelector(cc, 23, input[4]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Capture Channel Dropdown":"")};

        result = reactDropdownSelector(tl, 3, input[5]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Transaction Location Dropdown":"")};

        result = reactDropdownSelector(tt1, 3, input[6]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Transaction Tariff Dropdown":"")};

        result = reactDropdownSelector(mcc, 880, input[7]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in MCC Code Dropdown":"")};

        result = reactDropdownSelector(st, 9, input[8]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Settlement Type Dropdown":"")};

        result = reactDropdownSelector(co, 2, input[9]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Currency Option Dropdown":"")};

        result = reactDropdownSelector(et, 36, input[10]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Event Type Dropdown":"")};

        result = reactDropdownSelector(fc, 9, input[11]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Fee Code Dropdown":"")};

        result = fillInputField(per, input[12]);
        if(!result.equals("")) return new String[]{"failed", result};
        result = fillInputField(base, input[13]);
        if(!result.equals("")) return new String[]{"failed", result};
        result = fillInputField(min, input[14]);
        if(!result.equals("")) return new String[]{"failed", result};
        result = fillInputField(max, input[15]);
        if(!result.equals("")) return new String[]{"failed", result};

        return new String[]{"passed", ""};
    }

    private static String[] feeForm2Filler(String[] input) throws InterruptedException {
        String pc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[2]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String pf = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[3]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String ld = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[5]/div/div[2]/div/div/div/div/input";
        String et = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[6]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String fc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[8]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String base = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div/div/div[2]/div/div/div/div/input";

        String result;

        result = reactDropdownSelector(pc, 168, input[0]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Currency Dropdown":"")};

        result = reactDropdownSelector(pf, 9, input[1]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Frequency Dropdown":"")};

        result = fillInputField(ld, input[2]);
        if(!result.equals("")) return new String[]{"failed", result};

        result = reactDropdownSelector(et, 26, input[3]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Event Type Dropdown":"")};

        result = reactDropdownSelector(fc, 36, input[4]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Fee Code Dropdown":"")};

        result = fillInputField(base, input[5]);
        if(!result.equals("")) return new String[]{"failed", result};

        return new String[]{"passed", ""};
    }

    private static String[] feeForm3Filler(String[] input) throws InterruptedException {
        String pc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[2]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String pf = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[3]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String ld = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[4]/div/div[2]/div/div/div/div/input";
        String fc = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[1]/div[6]/div/div/div[2]/div/div/div[1]/div[2]/input";
        String base = "/html/body/div[1]/div/div[3]/div/div[2]/div[2]/div/div/div[2]/div/div/div[2]/div/div/div/div[2]/div/div/div[2]/div/div/div/div/input";

        String result;

        result = reactDropdownSelector(pc, 168, input[0]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Currency Dropdown":"")};

        result = reactDropdownSelector(pf, 9, input[1]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Posting Frequency Dropdown":"")};

        result = fillInputField(ld, input[2]);
        if(!result.equals("")) return new String[]{"failed", result};

        result = reactDropdownSelector(fc, 36, input[3]);
        if(!result.equals("")) return new String[]{"failed", result+(result.endsWith("found ")?"in Fee Code Dropdown":"")};

        result = fillInputField(base, input[4]);
        if(!result.equals("")) return new String[]{"failed", result};

        return new String[]{"passed", ""};
    }

    private static String reactDropdownSelector(String xpath, int limit, String value) throws InterruptedException {
        if(value.equals("")) {return "";}
//        System.out.println(value);
        driver.findElement(By.xpath(xpath)).click();
        String s = driver.findElement(By.xpath(xpath)).getAttribute("aria-controls").replace("listbox", "option-");
        Thread.sleep(200);
        int i;
        for(i = 1; i <= limit; i++) {
            if(driver.findElement(By.id(s+i)).getText().equals(value)) {
                driver.findElement(By.id(s+i)).click();
                break;
            }
        }
        Thread.sleep(1000);
        String res = checkForErrorToast();
        return !res.equals("")? res : i == limit+1? value+" not found ": "";
    }

    private static void getBackToCT() throws InterruptedException {
        if(driver.getCurrentUrl().equals("https://d5p05ae16v0dx.cloudfront.net/contractingtemplates/contractdetails")) {
            Thread.sleep(1000);
            executeNoAlertOnReload();
            driver.get("https://d5p05ae16v0dx.cloudfront.net/contractingtemplates/contractdetails");
            waitForSPLoader();
        } else {
            Thread.sleep(2000);
            driver.findElement(By.id("commonMenu")).click();
            Thread.sleep(200);
            driver.findElement(By.xpath("/html/body/div[2]/div[3]/ul/div/ul/li[2]")).click();
            waitForSPLoader();
        }
    }

    private static void getBackToFees(String url) throws InterruptedException {
        executeNoAlertOnReload();
        driver.get(url);
        Thread.sleep(2000);
        waitForSPLoader();
        waitForSPLoader();
        Thread.sleep(2000);
    }
}
