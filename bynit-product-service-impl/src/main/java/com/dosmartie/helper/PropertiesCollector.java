package com.dosmartie.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class PropertiesCollector {
    @Value("${auth.client.secret.id}")
    private String authId;
    public String getAuthId() {
        return authId;
    }
}
