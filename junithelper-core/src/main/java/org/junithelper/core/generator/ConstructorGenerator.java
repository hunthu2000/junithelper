/* 
 * Copyright 2009-2010 junithelper.org. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */
package org.junithelper.core.generator;

import java.util.List;

import org.junithelper.core.meta.ClassMeta;
import org.junithelper.core.meta.ConstructorMeta;

public interface ConstructorGenerator {

	List<String> getAllInstantiationSourceCodeList(ClassMeta classMeta);

	String getFirstInstantiationSourceCode(ClassMeta classMeta);

	String getInstantiationSourceCode(ClassMeta classMeta,
			ConstructorMeta constructorMeta);

}
