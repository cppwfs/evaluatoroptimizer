/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.evaluatoroptimizer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Main application class for the Evaluator Optimizer application.
 * This application implements the evaluator-optimizer pattern using Spring Integration.
 */
@SpringBootApplication
public class EvaluatorOptimizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvaluatorOptimizerApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(MessageChannel inputChannel) {
		return args -> {
			inputChannel.send(MessageBuilder.withPayload("""
					<user input>
					Implement a Stack in Java with:
					1. push(x)
					2. pop()
					3. getMin()
					All operations should be O(1).
					All inner fields should be private and when used should be prefixed with 'this.'.
					</user input>
					""").build());
		};
	}

}
