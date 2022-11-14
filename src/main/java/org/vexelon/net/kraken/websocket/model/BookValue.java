package org.vexelon.net.kraken.websocket.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BookValue implements Comparable<BookValue>{

    private BigDecimal price;
    private BigDecimal volume;

    @Override
    public int compareTo(BookValue o) {
        int i = this.price.compareTo(o.price);
        if(i != 0) return -i;  // reverse sort

        return this.price.compareTo(o.price);
    }
}
