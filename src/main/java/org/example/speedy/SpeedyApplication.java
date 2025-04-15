package org.example.speedy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.logging.Filter;

@SpringBootApplication
public class SpeedyApplication {

    /**
     * 
     * 
     * {
     * "combinator": "and",
     * "entity_type": "entity",
     * "rules": [
     * 	    {
     * 		"field": "some field",
     * 		"operator": "some_operator",
     * 		"value": "some_value"
     * }
     * ]
     * }
     *
     * // actions config
     * {
     * 	"action": "transform",
     * 	"entity_type": "entity",
     * "replacement_value": "some_value"
     * 	"field": "some_field"
     * }
     * 
     * @param args
     */
    enum Action {
        TRANSFORM, OTHER;
    }
    
    static class ActionConfig{
        Action action;
        EntityType entityType;
        String field;
        Object replacementValue;
        Object newValue;
        Map<String, Object> metadata = new HashMap<>();
    }
    
    enum Condition {
        AND,  OR
    }
    
    enum EntityType {
        PRODUCT    
    }
    
    static class FilterConfig{
        Condition condition;
        EntityType entityType;
        List<Rule> rule;
    }
    
    enum Operator {
        GREATER_THAN, CONTAINS
    }

    static class Rule{
        String field;
        Operator operator;
        Object value;
    }


    static class ActionResult{
        ActionConfig config;
        String errorMessage;
        Object transformedValue;

    }
    static class ActionManager {
        ActionResult applyAction(ActionConfig config){
            ActionResult result = new ActionResult();
            result.config = config;
            if(config.action == Action.TRANSFORM){
                result.transformedValue=config.newValue;
            }else{
                result.errorMessage= "No transformer found";
            }
            return result;
        }

    }

    public static void main(String[] args) {
        ActionConfig config = new ActionConfig();
        config.action = Action.TRANSFORM;
        config.entityType = EntityType.PRODUCT;
        config.replacementValue = 10D;
        config.newValue = 20D;
        config.field="amount";

        FilterEngine filterEngine = new FilterEngine();
        FilterConfigCreateRequest request = new FilterConfigCreateRequest();
        request.condition = Condition.AND;
        request.entitType = EntityType.PRODUCT;
        request.rule = new ArrayList<>();
        Rule rule = new Rule();
        rule.field = "amount";
        rule.operator = Operator.GREATER_THAN;
        rule.value = 9D;
        request.rule.add(rule);
        filterEngine.createFilter(request);



        ActionConfig config1 = filterEngine.applyFilters(config);
        if(Objects.isNull(config1)){
            System.out.println("Action config not satistfying the conditions");
            return;
        }

        ActionManager manager = new ActionManager();
        ActionResult actionResult = manager.applyAction(config1);
        System.out.println(actionResult.transformedValue);
        System.out.println(actionResult.errorMessage);
        return;
//        SpringApplication.run(SpeedyApplication.class, args);
    }

    static class FilterConfigCreateRequest{
        Condition condition;
        EntityType entitType;
        List<Rule> rule;
    }
    
    
    static class FilterEngine {
        Map<EntityType, List<FilterConfig>> filters=new HashMap<>();
        
        public boolean createFilter(FilterConfigCreateRequest request){
            String filterId = UUID.randomUUID().toString();
            FilterConfig filterConfig = createFilterConfig(request);
            if(filters.containsKey(request.entitType)){
                filters.get(request.entitType).add(filterConfig);
            }else{
                filters.computeIfAbsent(request.entitType, k -> {
                    List list = new ArrayList<>();
                    list.add(filterConfig);
                    return list;
                });
            }
            return true;
        }


        public ActionConfig applyFilters(ActionConfig config){
            if(Objects.isNull(config)){
                return null;
            }

            if(Objects.isNull(config.entityType)){
                return null;
            }

            boolean result = true;

            List<FilterConfig> filterConfigs = filters.get(config.entityType);

            for(FilterConfig filterConfig : filterConfigs){
                   List<Rule> rules = filterConfig.rule;
                   for(Rule rule : rules){
                       if(rule.field.equals(config.field)){
                           result = checkFilterCriteria(rule, config);
                           if(!result){
                               return null;
                           }
                       }
                   }
            }
            return config;
        }

        private boolean checkFilterCriteria(Rule rule, ActionConfig config) {
            if(rule.operator.equals(Operator.GREATER_THAN)){
                if(rule.value instanceof Double && config.replacementValue instanceof Double){
                    return (Double) config.replacementValue >= (Double) rule.value;
                }
            }
            if(rule.operator.equals(Operator.CONTAINS)){
                if(rule.value instanceof String && config.replacementValue instanceof String){
                    return true;
                }
            }
            return false;
        }

        private FilterConfig createFilterConfig(FilterConfigCreateRequest request) {
            FilterConfig filterConfig = new FilterConfig();
            filterConfig.condition = request.condition;
            filterConfig.entityType = request.entitType;
            filterConfig.rule = request.rule;
            return filterConfig;
        }
    }

}
