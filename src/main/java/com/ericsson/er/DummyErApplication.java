/*
   Copyright 2017 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ericsson.er;

import com.ericsson.er.api.entities.Eiffel.EiffelEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class DummyErApplication {
    public static ConcurrentHashMap<String, EiffelEvent[]> eiffelEventRepositories = new ConcurrentHashMap<>();

    public static long serverStart = Instant.now().toEpochMilli();
    public static long startTicks = 20;
    public static final long msBetweenTick = 2000;

    public static void main(String[] args) {
        SpringApplication.run(DummyErApplication.class, args);
    }
}
