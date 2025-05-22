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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@EnableIntegration
@Configuration
public class EvaluatorOptimizerConfiguration {

    private static final String EVALUATION_RESPONSE = "evaluation-response";
    private static final String MEMORY = "memory";
    private static final String CHAIN_OF_THOUGHT = "chain-of-thought";
    private static final String CONTEXT = "context";

    @Bean
    public IntegrationFlow evaluatorOptimizerFlow(AbstractTransformer generateCodeTransformer) {
        return IntegrationFlow.from("inputChannel")
                .transform(generateCodeTransformer)
                .routeToRecipients(r -> {
                    r.recipientMessageSelector("inputChannel",
                            m -> ((EvaluationResponse) Objects.requireNonNull(m.getHeaders().get(EVALUATION_RESPONSE)))
                                    .evaluation().equals(EvaluationResponse.Evaluation.NEEDS_IMPROVEMENT))
                            .defaultOutputToParentFlow();
                })
                .handle(m -> System.out.println("FINAL OUTPUT:\n : " + m.getPayload()))
                .get();
    }

    @Bean
    AbstractTransformer generateCodeTransformer(GeneratorEvaluator generatorEvaluator) {
        return new AbstractTransformer() {
            @Override
            protected Object doTransform(Message<?> message) {
                List<String> memory = message.getHeaders().containsKey(MEMORY)?(List<String>) message.getHeaders().get(MEMORY) : new ArrayList<>();
                List<Generation> chainOfThought = message.getHeaders().containsKey(CHAIN_OF_THOUGHT)?(List<Generation>) message.getHeaders().get(CHAIN_OF_THOUGHT) : new ArrayList<>();
                String task  = message.getPayload().toString();
                String context =  message.getHeaders().containsKey(CONTEXT)?(String) message.getHeaders().get(CONTEXT) : "";
                Generation generation = generatorEvaluator.generate(task, context);
                memory.add(generation.response());
                chainOfThought.add(generation);

                EvaluationResponse evaluationResponse = generatorEvaluator.evalute(generation.response(), task);

                if (evaluationResponse.evaluation().equals(EvaluationResponse.Evaluation.PASS)) {
                    // Solution is accepted!
                    return MessageBuilder.withPayload(new RefinedResponse(generation.response(), chainOfThought))
                            .setHeaderIfAbsent(EVALUATION_RESPONSE, evaluationResponse)
                            .build();
                }
                // Accumulated new context including the last and the previous attempts and
                // feedbacks.
                StringBuilder newContext = new StringBuilder();
                newContext.append("Previous attempts:");
                for (String m : memory) {
                    newContext.append("\n- ").append(m);
                }
                newContext.append("\nFeedback: ").append(evaluationResponse.feedback());
               return  MessageBuilder.withPayload(task).setHeaderIfAbsent(CONTEXT, newContext.toString())
                       .setHeaderIfAbsent(MEMORY, memory)
                       .setHeaderIfAbsent(CHAIN_OF_THOUGHT, chainOfThought)
                       .setHeaderIfAbsent(EVALUATION_RESPONSE, evaluationResponse)
                       .build();
            }
        };
    }

    @Bean
    GeneratorEvaluator generatorEvaluator(ChatClient.Builder chatClientBuilder) {
        return new GeneratorEvaluator(chatClientBuilder.build());
    }

}
