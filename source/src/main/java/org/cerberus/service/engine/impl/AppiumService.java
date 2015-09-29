/*
 * Cerberus  Copyright (C) 2013  vertigo17
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.service.engine.impl;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import java.util.Set;
import org.apache.log4j.Level;
import org.cerberus.crud.entity.Identifier;
import org.cerberus.crud.entity.MessageEvent;
import org.cerberus.crud.entity.Session;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.log.MyLogger;
import org.cerberus.service.engine.IAppiumService;
import org.cerberus.util.ParameterParserUtil;
import org.cerberus.util.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

/**
 *
 * @author bcivel
 */
@Service
public class AppiumService implements IAppiumService {

    @Override
    public MessageEvent switchToContext(Session session, Identifier identifier) {
        MessageEvent message;
        AppiumDriver driver = session.getAppiumDriver();
        String newContext = "";
        Set<String> contextNames = driver.getContextHandles();
        for (String contextName : contextNames) {
            System.out.println("context"+contextName);
            if (contextName.contains("WEBVIEW")) {
                driver.context(contextName);
                newContext = contextName;
                break;
            }
        }
        //driver.context("WEBVIEW_1");
        message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_SWITCHTOWINDOW);
        message.setDescription(message.getDescription().replaceAll("%WINDOW%", newContext));
        return message;
    }
    
    @Override
    public MessageEvent doActionType(Session session, Identifier identifier, String property, String propertyName) {
        MessageEvent message;
        //System.out.print(session.getDriver().getWindowHandles());
        try {
            //WebElement webElement = this.getElement(session, identifier, true, true);
            //webElement.click();
            WebDriverWait wait = new WebDriverWait(session.getAppiumDriver(), session.getDefaultWait());
            if (!StringUtil.isNull(property)) {
//                wait.until(ExpectedConditions.elementToBeClickable(this.getBy(identifier)))
//                .click();
                TouchAction action = new TouchAction(session.getAppiumDriver());
                action.press(wait.until(ExpectedConditions.elementToBeClickable(this.getBy(identifier)))).release().perform();
                session.getAppiumDriver().getKeyboard().pressKey(property);
                //webElement.sendKeys(property);
            }
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_TYPE);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", identifier.getIdentifier() + "=" + identifier.getLocator()));
            if (!StringUtil.isNull(property)) {
                message.setDescription(message.getDescription().replaceAll("%DATA%", ParameterParserUtil.securePassword(property, propertyName)));
            } else {
                message.setDescription(message.getDescription().replaceAll("%DATA%", "No property"));
            }
            return message;
        } catch (NoSuchElementException exception) {
            message = new MessageEvent(MessageEventEnum.ACTION_FAILED_TYPE_NO_SUCH_ELEMENT);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", identifier.getIdentifier() + "=" + identifier.getLocator()));
            MyLogger.log(WebDriverService.class.getName(), Level.DEBUG, exception.toString());
            return message;
        } catch (WebDriverException exception) {
            message = new MessageEvent(MessageEventEnum.ACTION_FAILED_SELENIUM_CONNECTIVITY);
            MyLogger.log(WebDriverService.class.getName(), Level.FATAL, exception.toString());
            return message;
        }
    }
    
    private By getBy(Identifier identifier) {

        MyLogger.log(RunTestCaseService.class.getName(), Level.DEBUG, "Finding selenium Element : " + identifier.getLocator() + " by : " + identifier.getIdentifier());

        if (identifier.getIdentifier().equalsIgnoreCase("id")) {
            return By.id(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("name")) {
            return By.name(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("class")) {
            return By.className(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("css")) {
            return By.cssSelector(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("xpath")) {
            return By.xpath(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("link")) {
            return By.linkText(identifier.getLocator());

        } else if (identifier.getIdentifier().equalsIgnoreCase("data-cerberus")) {
            return By.xpath("//*[@data-cerberus='" + identifier.getLocator() + "']");

        } else {
            throw new NoSuchElementException(identifier.getIdentifier());
        }
    }

    private WebElement getElement(Session session, Identifier identifier, boolean visible, boolean clickable) {
        AppiumDriver driver = session.getAppiumDriver();
        By locator = this.getBy(identifier);
        
        MyLogger.log(RunTestCaseService.class.getName(), Level.DEBUG, "Waiting for Element : " + identifier.getIdentifier() + "=" + identifier.getLocator());
        try {
            WebDriverWait wait = new WebDriverWait(driver, session.getDefaultWait());
            if (visible) {
                if (clickable) {
                    wait.until(ExpectedConditions.elementToBeClickable(locator));
                } else {
                    wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                }
            } else {
                wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            }
        } catch (TimeoutException exception) {
            MyLogger.log(RunTestCaseService.class.getName(), Level.FATAL, "Exception waiting for element :" + exception);
            throw new NoSuchElementException(identifier.getIdentifier() + "=" + identifier.getLocator());
        }
        MyLogger.log(RunTestCaseService.class.getName(), Level.DEBUG, "Finding Element : " + identifier.getIdentifier() + "=" + identifier.getLocator());
        return driver.findElement(locator);
    }
    
    @Override
    public MessageEvent doActionClick(Session session, Identifier identifier, boolean waitForVisibility, boolean waitForClickability) {
        MessageEvent message;
        try {
            WebDriverWait wait = new WebDriverWait(session.getAppiumDriver(), session.getDefaultWait());
//            wait.until(ExpectedConditions.elementToBeClickable(this.getBy(identifier)))
//                .click();
            TouchAction action = new TouchAction(session.getAppiumDriver());
                action.press(wait.until(ExpectedConditions.elementToBeClickable(this.getBy(identifier)))).release().perform();
                
                
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_CLICK);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", identifier.getIdentifier() + "=" + identifier.getLocator()));
            return message;
        } catch (NoSuchElementException exception) {
            message = new MessageEvent(MessageEventEnum.ACTION_FAILED_CLICK_NO_SUCH_ELEMENT);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", identifier.getIdentifier() + "=" + identifier.getLocator()));
            MyLogger.log(WebDriverService.class.getName(), Level.DEBUG, exception.toString());
            return message;
        } catch (WebDriverException exception) {
            message = new MessageEvent(MessageEventEnum.ACTION_FAILED_SELENIUM_CONNECTIVITY);
            MyLogger.log(WebDriverService.class.getName(), Level.FATAL, exception.toString());
            return message;
        }

    }
}