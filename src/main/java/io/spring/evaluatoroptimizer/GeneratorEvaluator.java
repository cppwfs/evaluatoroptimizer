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

import org.springframework.ai.chat.client.ChatClient;

public class GeneratorEvaluator {

    public static final String DEFAULT_GENERATOR_PROMPT = """
			Your goal is to complete the task based on the input. If there are feedback
			from your previous generations, you should reflect on them to improve your solution.

			CRITICAL: Your response must be a SINGLE LINE of valid JSON with NO LINE BREAKS except those explicitly escaped with \\n.
			Here is the exact format to follow, including all quotes and braces:

			{"thoughts":"Brief description here","response":"public class Example {\\n    // Code here\\n}"}

			Rules for the response field:
			1. ALL line breaks must use \\n
			2. ALL quotes must use \\"
			3. ALL backslashes must be doubled: \\
			4. NO actual line breaks or formatting - everything on one line
			5. NO tabs or special characters
			6. Java code must be complete and properly escaped

			Example of properly formatted response:
			{"thoughts":"Implementing counter","response":"public class Counter {\\n    private int count;\\n    public Counter() {\\n        count = 0;\\n    }\\n    public void increment() {\\n        count++;\\n    }\\n}"}

			Follow this format EXACTLY - your response must be valid JSON on a single line.
			""";

    public static final String DEFAULT_EVALUATOR_PROMPT = """
			Evaluate this code implementation for correctness, time complexity, and best practices.
			Ensure the code have proper javadoc documentation.
			Respond with EXACTLY this JSON format on a single line:

			{"evaluation":"PASS, NEEDS_IMPROVEMENT, or FAIL", "feedback":"Your feedback here"}

			The evaluation field must be one of: "PASS", "NEEDS_IMPROVEMENT", "FAIL"
			Use "PASS" only if all criteria are met with no improvements needed.
			""";

    private ChatClient chatClient;

    public GeneratorEvaluator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Evaluates if a solution meets the specified requirements and quality
     * criteria.
     * This method represents the evaluator component of the workflow, analyzing
     * solutions
     * and providing detailed feedback for further refinement until the desired
     * quality
     * level is reached.
     *
     * @param content The solution content to be evaluated
     * @param task    The original task against which to evaluate the solution
     * @return An EvaluationResponse containing the evaluation result
     *         (PASS/NEEDS_IMPROVEMENT/FAIL)
     *         and detailed feedback for improvement
     */
    public  EvaluationResponse evalute(String content, String task) {

        EvaluationResponse evaluationResponse = chatClient.prompt()
                .user(u -> u.text("{prompt}\nOriginal task: {task}\nContent to evaluate: {content}")
                        .param("prompt", DEFAULT_EVALUATOR_PROMPT)
                        .param("task", task)
                        .param("content", content))
                .call()
                .entity(EvaluationResponse.class);

        System.out.println(String.format("\n=== EVALUATOR OUTPUT ===\nEVALUATION: %s\n\nFEEDBACK: %s\n",
                evaluationResponse.evaluation(), evaluationResponse.feedback()));
        return evaluationResponse;
    }

    /**
     * Generates or refines a solution based on the given task and feedback context.
     * This method represents the generator component of the workflow, producing
     * responses that can be iteratively improved through evaluation feedback.
     *
     * @param task    The primary task or problem to be solved
     * @param context Previous attempts and feedback for iterative improvement
     * @return A Generation containing the model's thoughts and proposed solution
     */
    public Generation generate(String task, String context) {
        Generation generationResponse = chatClient.prompt()
                .user(u -> u.text("{prompt}\n{context}\nTask: {task}")
                        .param("prompt", DEFAULT_GENERATOR_PROMPT)
                        .param("context", context)
                        .param("task", task))
                .call()
                .entity(Generation.class);

        System.out.println(String.format("\n=== GENERATOR OUTPUT ===\nTHOUGHTS: %s\n\nRESPONSE:\n %s\n",
                generationResponse.thoughts(), generationResponse.response()));
        return generationResponse;
    }
}
