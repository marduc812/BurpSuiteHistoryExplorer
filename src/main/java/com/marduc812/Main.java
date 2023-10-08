package com.marduc812;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.extension.Extension;


public class Main implements BurpExtension {

    MontoyaApi api;
    Logging logging;


    @Override
    public void initialize(MontoyaApi api) {

        logging = api.logging();
        Extension extension = api.extension();

        extension.setName("History Explorer");
        logging.logToOutput("\"History Explorer\" by marduc812");
        logging.logToOutput("Github: https://github.com/marduc812/BurpHistoryExplorer");
        logging.logToOutput("=========================================================");
        logging.logToOutput("The extension was made for making filtering output per host easier. ");
        logging.logToOutput("The output is presented inside a table filtered per host.");
        logging.logToOutput("=========================================================");
        logging.logToOutput("Extension filtering");
        logging.logToOutput("To filter for requests that have no extension use the \"none\" keyword.");

        api.userInterface().registerSuiteTab("History Explorer", new HistoryExplorerGui(api));

    }
}