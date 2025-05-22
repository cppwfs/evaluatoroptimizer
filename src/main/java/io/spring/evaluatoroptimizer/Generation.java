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

/**
 * Represents a solution generation step. Contains the model's thoughts and the
 * proposed solution.
 *
 * @param thoughts The model's understanding of the task and feedback
 * @param response The model's proposed solution
 */
public record Generation(String thoughts, String response) {

}