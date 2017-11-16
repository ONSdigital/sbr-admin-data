package model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a definition of admin record for a reference period i.e. a year and month
 * Each unit is compose of an id and a map of key value pairs reprsenting the variables
 *
 */
public class AdminData {

    public static final String REFERENCE_PERIOD_FORMAT = "yyyyMM";

    @JsonProperty("period")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = REFERENCE_PERIOD_FORMAT)
    private YearMonth referencePeriod;

    @JsonProperty("id")
    private String id;

    @JsonProperty("vars")
    private Map<String, String> variables;

    public AdminData(YearMonth referencePeriod, String id) {
        this.referencePeriod = referencePeriod;
        this.id = id;
        this.variables = new HashMap<>();
    }

    public YearMonth getReferencePeriod() {
        return referencePeriod;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void putVariable(String variable, String value) {
        this.variables.put(variable, value);
    }

}
