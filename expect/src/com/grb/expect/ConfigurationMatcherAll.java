package com.grb.expect;


public class ConfigurationMatcherAll implements ConfigurationMatcher {
    
    public final static ConfigurationMatcherAll onlyInstance = new ConfigurationMatcherAll();
    
    private ConfigurationMatcherAll() {   
    }
    
    public boolean configurationMatches(Object ctx) {
        return true;
    }
}
