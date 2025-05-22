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

/**
 * Configuration class for the Evaluator Optimizer application.
 * This class defines the integration flow and beans required for the evaluator-optimizer pattern.
 */
@EnableIntegration
@Configuration
public class EvaluatorOptimizerConfiguration {

    /**
     * Header name for the evaluation response.
     */
    private static final String EVALUATION_RESPONSE = "evaluation-response";

    /**
     * Header name for the memory of previous attempts.
     */
    private static final String MEMORY = "memory";

    /**
     * Header name for the chain of thought.
     */
    private static final String CHAIN_OF_THOUGHT = "chain-of-thought";

    /**
     * Header name for the context.
     */
    private static final String CONTEXT = "context";

    /**
     * Creates the main integration flow for the evaluator-optimizer pattern.
     * This flow processes input messages, transforms them using the generateCodeTransformer,
     * routes messages based on the evaluation response, and handles the final output.
     *
     * @param generateCodeTransformer The transformer that generates and evaluates code
     * @return An IntegrationFlow that implements the evaluator-optimizer pattern
     */
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

    /**
     * Creates a transformer that generates and evaluates code based on the input message.
     * This transformer processes the input message, generates a solution using the generatorEvaluator,
     * evaluates the solution, and returns either the final solution or a message with feedback for further refinement.
     *
     * @param generatorEvaluator The service that generates and evaluates code
     * @return An AbstractTransformer that transforms input messages into solutions or feedback
     */
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

    /**
     * Creates a GeneratorEvaluator bean that is responsible for generating and evaluating code.
     * This bean uses a ChatClient to interact with the AI model for code generation and evaluation.
     *
     * @param chatClientBuilder The builder for creating a ChatClient
     * @return A GeneratorEvaluator that can generate and evaluate code
     */
    @Bean
    GeneratorEvaluator generatorEvaluator(ChatClient.Builder chatClientBuilder) {
        return new GeneratorEvaluator(chatClientBuilder.build());
    }

}
