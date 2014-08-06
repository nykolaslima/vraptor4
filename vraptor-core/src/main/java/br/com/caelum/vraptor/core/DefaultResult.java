/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.caelum.vraptor.core;


import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.View;
import br.com.caelum.vraptor.interceptor.TypeNameExtractor;
import br.com.caelum.vraptor.ioc.Container;
import br.com.caelum.vraptor.validator.Validator;

/**
 * A basic implementation of a Result
 * @author guilherme silveira
 */
@RequestScoped
public class DefaultResult extends AbstractResult {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultResult.class);

	private final HttpServletRequest request;
	private final Container container;
	private final ExceptionMapper exceptions;
	private final TypeNameExtractor extractor;
	private Validator validator;
	
	private Map<String, Object> includedAttributes;
	private boolean responseCommitted = false;
	

	/** 
	 * @deprecated CDI eyes only
	 */
	protected DefaultResult() {
		this(null, null, null, null, null);
	}

	@Inject
	public DefaultResult(HttpServletRequest request, Container container, ExceptionMapper exceptions, TypeNameExtractor extractor,
			Validator validator) {
		this.request = request;
		this.container = container;
		this.extractor = extractor;
		this.includedAttributes = new HashMap<>();
		this.exceptions = exceptions;
		this.validator = validator;
	}
	
	@Override
	public <T extends View> T use(Class<T> view) {
		throwExceptionIfValidatorHasErrors();
	    
		responseCommitted = true;
		return container.instanceFor(view);
	}
	
	@Override
	public Result on(Class<? extends Exception> exception) {
		return exceptions.record(exception);
	}

	@Override
	public Result include(String key, Object value) {
		logger.debug("including attribute {}: {}", key, value);
		
		includedAttributes.put(key, value);
		request.setAttribute(key, value);
		return this;
	}

	@Override
	public boolean used() {
		return responseCommitted;
	}

	@Override
	public Map<String, Object> included() {
		return unmodifiableMap(includedAttributes);
	}

	@Override
	public Result include(Object value) {
		if(value == null) {
			return this;
		}
		
		String key = extractor.nameFor(value.getClass());
		return include(key, value);
	}
	
	private void throwExceptionIfValidatorHasErrors() {
		if(validator.hasErrors()) {
			throw new IllegalStateException(
					"There are validation errors and you forgot to specify where to go. Please add in your method "
							+ "something like:\n"
							+ "validator.onErrorUse(page()).of(AnyController.class).anyMethod();\n"
							+ "or any view that you like.\n"
							+ "If you didn't add any validation error, it is possible that a conversion error had happened.");
		}
	}
}
