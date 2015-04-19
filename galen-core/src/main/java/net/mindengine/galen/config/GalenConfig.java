/*******************************************************************************
* Copyright 2015 Ivan Shubin http://mindengine.net
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/
package net.mindengine.galen.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import net.mindengine.galen.specs.SpecImage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalenConfig {

    private final static Logger LOG = LoggerFactory.getLogger(GalenConfig.class);
    
    public final static GalenConfig instance = new GalenConfig();
    public static final String SCREENSHOT_AUTORESIZE = "galen.screenshot.autoresize";
    public static final String SCREENSHOT_FULLPAGE = "galen.browser.screenshots.fullPage";
    // smart waiting for scroll position, but with a timeout, set to zero to turn off smart wait
    public static final String SCREENSHOT_FULLPAGE_SCROLLTIMEOUT= "galen.browser.screenshots.fullPage.scrollTimeout";
    // hard wait during scroll
    public static final String SCREENSHOT_FULLPAGE_SCROLLWAIT = "galen.browser.screenshots.fullPage.scrollWait";
    public static final String SPEC_IMAGE_TOLERANCE = "galen.spec.image.tolerance";
    public static final String SPEC_IMAGE_ERROR_RATE = "galen.spec.image.error";
    public static final String SPEC_GLOBAL_VISIBILITY_CHECK = "galen.spec.global.visibility";

    public static final String TEST_JS_SUFFIX = "galen.test.js.file.suffix";
    public static final String TEST_SUFFIX = "galen.test.file.suffix";
    public static final String GALEN_CONFIG_FILE = "galen.config.file";
    public static final String GALEN_RANGE_APPROXIMATION = "galen.range.approximation";
    public static final String GALEN_REPORTING_LISTENERS = "galen.reporting.listeners";
    public static final String GALEN_DEFAULT_BROWSER = "galen.default.browser";
    public static final String GALEN_LOG_LEVEL = "galen.log.level";
    public static final String GALEN_USE_FAIL_EXIT_CODE = "galen.use.fail.exit.code";
    private int rangeApproximation;
    private List<String> reportingListeners;
    private String defaultBrowser;
    private Properties properties;
    
    private GalenConfig() {
        try {
            loadConfig();
        }
        catch (Exception e) {
            LOG.trace("Unknown error during Galen Config", e);
        }
    }
    
    private void loadConfig() throws IOException {
        this.properties = new Properties();

        File configFile = new File(System.getProperty(GALEN_CONFIG_FILE, "config"));
        
        if (configFile.exists() && configFile.isFile()) {
            InputStream in = new FileInputStream(configFile);
            properties.load(in);
            in.close();
        }

        rangeApproximation = Integer.parseInt(readProperty(GALEN_RANGE_APPROXIMATION, "2"));
        reportingListeners = converCommaSeparatedList(readProperty(GALEN_REPORTING_LISTENERS, ""));
        defaultBrowser = readProperty(GALEN_DEFAULT_BROWSER, "firefox");
    }

    private List<String> converCommaSeparatedList(String text) {
        String[] arr = text.split(",");
        
        List<String> list = new LinkedList<String>();
        for (String item : arr) {
            String itemText = item.trim();
            if (!itemText.isEmpty()) {
                list.add(itemText);
            }
        }
        return list;
    }

    public String readProperty(String name, String defaultValue) {
        return properties.getProperty(name, System.getProperty(name, defaultValue));
    }
    
    public String readProperty(String name) {
        return properties.getProperty(name, System.getProperty(name));
    }
    
    public String readMandatoryProperty(String name) {
        String value = properties.getProperty(name, System.getProperty(name));
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing property: " + name);
        }
        return value;
    }


    public synchronized static GalenConfig getConfig() {
        return instance;
    }
    
    public void reset() throws IOException {
        loadConfig();
    }

    public int getRangeApproximation() {
        return this.rangeApproximation;
    }

    public List<String> getReportingListeners() {
        return this.reportingListeners;
    }

    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    public Integer getIntProperty(String name, int defaultValue) {
        String value = readProperty(name);
        if (value == null) {
            return defaultValue;
        }
        else {
            try {
                return Integer.parseInt(value);
            }
            catch (Exception e) {
                throw new RuntimeException(String.format("Couldn't parse property \"%s\" from config file", name));
            }
        }
    }

    
    public int getIntProperty(String name, int defaultValue, int min, int max) {
        int value = getIntProperty(name, defaultValue);
        if (value >= min && value <=max) {
            return value;
        }
        else {
            throw new RuntimeException(String.format("Property \"%s\"=%d in config file is not in allowed range [%d, %d]", name, value, min, max));
        }
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = readProperty(name);
        if (value == null) {
            return defaultValue;
        }
        else {
            return Boolean.parseBoolean(value);
        }
    }

    public int getLogLevel() {

        String value = readProperty(GALEN_LOG_LEVEL, "10");
        if (StringUtils.isNumeric(value)) {
            return Integer.parseInt(value);
        }
        else return 10;
    }
    
    public boolean getUseFailExitCode() {

        String value = readProperty(GALEN_USE_FAIL_EXIT_CODE);
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        else return false;
    }

    public String getTestJsSuffix() {
        return properties.getProperty(TEST_JS_SUFFIX, ".test.js");
    }

    public boolean shouldAutoresizeScreenshots() {
        return getBooleanProperty(GalenConfig.SCREENSHOT_AUTORESIZE, true);
    }

    public boolean shouldCheckVisibilityGlobally() {
        return getBooleanProperty(GalenConfig.SPEC_GLOBAL_VISIBILITY_CHECK, true);
    }

    public int getImageSpecDefaultTolerance() {
        return getIntProperty(GalenConfig.SPEC_IMAGE_TOLERANCE, 25);
    }

    public SpecImage.ErrorRate getImageSpecDefaultErrorRate() {
        String errorRateText = readProperty(SPEC_IMAGE_ERROR_RATE, "0px");
        return SpecImage.ErrorRate.fromString(errorRateText);
    }

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getTestSuffix() {
        return properties.getProperty(TEST_SUFFIX, ".test");
    }
}
