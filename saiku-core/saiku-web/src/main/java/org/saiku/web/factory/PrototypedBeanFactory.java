package org.saiku.web.factory;

import java.util.HashMap;

import org.saiku.olap.util.formatter.ICellSetFormatter;
import org.saiku.olap.util.formatter.ICellSetFormatterFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Allows creation of customized "prototype" scope beans, implementing factory interfaces in Saiku modules.
 * Maps between bean key and bean id is configured with Spring.
 * @author nde
 *
 */
public class PrototypedBeanFactory implements ApplicationContextAware, ICellSetFormatterFactory {
    
    private ApplicationContext applicationContext;
    private HashMap<String, String> cellSetFormatterMap;
    private String defaultCellSetFormatterBeanId;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    public ICellSetFormatter getCellSetFormatter(String formatter) {
        String cellSetFormatterBeanId = cellSetFormatterMap.get(formatter);
        if (cellSetFormatterBeanId == null) {
            return (ICellSetFormatter) applicationContext.getBean(defaultCellSetFormatterBeanId);
        }
        return (ICellSetFormatter) applicationContext.getBean(cellSetFormatterBeanId);
    }
    
    public void setCellSetFormatterMap(HashMap<String, String> cellSetFormatterMap) {
        this.cellSetFormatterMap = cellSetFormatterMap;
    }
    public HashMap<String, String> getCellSetFormatterMap() {
        return cellSetFormatterMap;
    }
    public String getDefaultCellSetFormatterBeanId() {
        return defaultCellSetFormatterBeanId;
    }
    public void setDefaultCellSetFormatterBeanId(String defaultCellSetFormatterBeanId) {
        this.defaultCellSetFormatterBeanId = defaultCellSetFormatterBeanId;
    }
    

}
