package com.igraded.locker;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockerConfig {

    /** Where the files go on disk. Set igraded.locker.dir to change it. */
    @Bean
    LockerStorage lockerStorage(@Value("${igraded.locker.dir:./data/locker}") String dir) {
        return new LockerStorage(Path.of(dir));
    }
}
