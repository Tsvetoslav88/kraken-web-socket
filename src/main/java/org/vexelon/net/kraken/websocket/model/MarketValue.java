package org.vexelon.net.kraken.websocket.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties
public class MarketValue {
    @JsonProperty
    public ArrayList<ArrayList<String>> as;
    @JsonProperty
    public ArrayList<ArrayList<String>> bs;
}
