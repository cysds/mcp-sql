package com.cysds.domain.job;

import com.cysds.domain.repository.ConnectionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.Callable;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-19 21:33
 **/
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteGenerateThreadTask implements Callable<ResponseEntity<StreamingResponseBody>> {

    private ConnectionRepository connectionRepository;

    private String message;

    @Override
    public ResponseEntity<StreamingResponseBody> call() throws Exception {
        return connectionRepository.execute(message);
    }
}
