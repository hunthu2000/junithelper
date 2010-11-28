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
package org.junithelper.core.generator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.junithelper.core.config.Configulation;
import org.junithelper.core.config.JUnitVersion;
import org.junithelper.core.config.MessageValue;
import org.junithelper.core.config.MockObjectFramework;
import org.junithelper.core.constant.RegExp;
import org.junithelper.core.constant.StringValue;
import org.junithelper.core.filter.TrimFilterUtil;
import org.junithelper.core.generator.TestCaseGenerator;
import org.junithelper.core.generator.TestMethodGenerator;
import org.junithelper.core.meta.AccessModifier;
import org.junithelper.core.meta.ClassMeta;
import org.junithelper.core.meta.ConstructorMeta;
import org.junithelper.core.meta.ExceptionMeta;
import org.junithelper.core.meta.MethodMeta;
import org.junithelper.core.meta.TestCaseMeta;
import org.junithelper.core.meta.TestMethodMeta;
import org.junithelper.core.meta.extractor.ClassMetaExtractor;

public class DefaultTestCaseGenerator implements TestCaseGenerator {

	private Configulation config;
	private ClassMeta targetClassMeta;
	private MessageValue messageValue = new MessageValue();
	private TestMethodGenerator testMethodGenerator;

	public DefaultTestCaseGenerator(Configulation config) {
		this.config = config;
		testMethodGenerator = new DefaultTestMethodGenerator(config);
	}

	@Override
	public DefaultTestCaseGenerator initialize(String targetSourceCodeString) {
		ClassMetaExtractor classMetaExtractor = new ClassMetaExtractor(config);
		this.targetClassMeta = classMetaExtractor
				.extract(targetSourceCodeString);
		this.testMethodGenerator.initialize(targetClassMeta);
		this.messageValue.initialize(config.language);
		return this;
	}

	@Override
	public DefaultTestCaseGenerator initialize(ClassMeta targetClassMeta) {
		this.targetClassMeta = targetClassMeta;
		this.testMethodGenerator.initialize(targetClassMeta);
		this.messageValue.initialize(config.language);
		return this;
	}

	@Override
	public TestCaseMeta getNewTestCaseMeta() {
		TestCaseMeta testCaseMeta = new TestCaseMeta();
		testCaseMeta.target = targetClassMeta;
		for (MethodMeta targetMethodMeta : testCaseMeta.target.methods) {
			testCaseMeta.tests.add(testMethodGenerator
					.getTestMethodMeta(targetMethodMeta));
		}
		return testCaseMeta;
	}

	@Override
	public List<TestMethodMeta> getLackingTestMethodMetaList(
			String currentTestCaseSourceCode) {

		List<TestMethodMeta> dest = new ArrayList<TestMethodMeta>();
		String checkTargetSourceCode = TrimFilterUtil
				.doAllFilters(currentTestCaseSourceCode);
		// type safe test
		if (!checkTargetSourceCode.matches(RegExp.Anything_ZeroOrMore_Min
				+ "public\\s+void\\s+[^\\s]*type\\("
				+ RegExp.Anything_ZeroOrMore_Min)) {
			TestMethodMeta testMethod = new TestMethodMeta();
			testMethod.classMeta = targetClassMeta;
			testMethod.isTypeTest = true;
			dest.add(testMethod);
		}
		// is testing instantiation required
		if (targetClassMeta.constructors.size() > 0) {
			ConstructorMeta notPrivateConstructor = null;
			for (ConstructorMeta constructor : targetClassMeta.constructors) {
				if (constructor.accessModifier != AccessModifier.Private) {
					notPrivateConstructor = constructor;
					break;
				}
			}
			// instantiation test
			if (notPrivateConstructor != null) {
				if (!checkTargetSourceCode
						.matches(RegExp.Anything_ZeroOrMore_Min
								+ "public\\s+void\\s+[^\\s]*instantiation\\("
								+ RegExp.Anything_ZeroOrMore_Min)) {
					TestMethodMeta testMethod = new TestMethodMeta();
					testMethod.classMeta = targetClassMeta;
					testMethod.isInstantiationTest = true;
					dest.add(testMethod);
				}
			}
		}
		// test methods
		for (MethodMeta methodMeta : targetClassMeta.methods) {
			TestMethodMeta testMethodMeta = new TestMethodMeta();
			testMethodMeta.methodMeta = methodMeta;
			String testMethodNamePrefix = testMethodGenerator
					.getTestMethodNamePrefix(testMethodMeta);
			// the test method is not already exist
			if (!checkTargetSourceCode.matches(RegExp.Anything_ZeroOrMore_Min
					+ testMethodNamePrefix.replaceAll("\\$", "\\\\\\$")
					+ RegExp.Anything_ZeroOrMore_Min)) {
				// exclude accessors
				if (config.target.isAccessorExcluded && methodMeta.isAccessor) {
					continue;
				}
				// testing target access modifier
				if ((methodMeta.accessModifier == AccessModifier.Public && config.target.isPublicMethodRequired)
						|| (methodMeta.accessModifier == AccessModifier.Protected && config.target.isProtectedMethodRequired)
						|| (methodMeta.accessModifier == AccessModifier.PackageLocal && config.target.isPackageLocalMethodRequired)) {
					dest.add(testMethodGenerator.getTestMethodMeta(methodMeta));
					if (config.target.isExceptionPatternRequired) {
						// testing exception patterns
						for (ExceptionMeta exceptionMeta : methodMeta.throwsExceptions) {
							dest.add(testMethodGenerator.getTestMethodMeta(
									methodMeta, exceptionMeta));
						}
					}
				}
			}
		}
		return dest;
	}

	@Override
	public String getNewTestCaseSourceCode() {
		StringBuilder buf = new StringBuilder();
		if (targetClassMeta.packageName != null) {
			buf.append("package ");
			buf.append(targetClassMeta.packageName);
			buf.append(";");
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
		}
		for (String imported : targetClassMeta.importedList) {
			buf.append("import ");
			buf.append(imported);
			buf.append(";");
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
		}
		if (config.junitVersion == JUnitVersion.version3) {
			buf.append("import ");
			buf.append(config.testCaseClassNameToExtend);
			buf.append(";");
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
		}
		buf.append(StringValue.CarriageReturn);
		buf.append(StringValue.LineFeed);
		buf.append("public class ");
		buf.append(targetClassMeta.name);
		buf.append("Test ");
		if (config.junitVersion == JUnitVersion.version3) {
			buf.append("extends ");
			String[] splittedArray = config.testCaseClassNameToExtend
					.split("\\.");
			buf.append(splittedArray[splittedArray.length - 1]);
			buf.append(" ");
		}
		buf.append("{");
		buf.append(StringValue.CarriageReturn);
		buf.append(StringValue.LineFeed);
		buf.append(StringValue.CarriageReturn);
		buf.append(StringValue.LineFeed);
		buf.append("}");
		buf.append(StringValue.CarriageReturn);
		buf.append(StringValue.LineFeed);
		return getTestCaseSourceCodeWithLackingTestMethod(buf.toString());
	}

	@Override
	public String getTestCaseSourceCodeWithLackingTestMethod(
			String currentTestCaseSourceCode) {
		String dest = currentTestCaseSourceCode;
		// lacking test methods
		StringBuilder buf = new StringBuilder();
		List<TestMethodMeta> lackingTestMethodMetaList = getLackingTestMethodMetaList(currentTestCaseSourceCode);
		if (lackingTestMethodMetaList.size() == 0) {
			// not modified
			return dest;
		}
		for (TestMethodMeta testMethodMeta : lackingTestMethodMetaList) {
			// method signature
			buf.append(testMethodGenerator
					.getTestMethodSourceCode(testMethodMeta));
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
		}
		dest = dest.replaceFirst("}[^}]*$", "");
		String lackingSourceCode = buf.toString();
		dest += lackingSourceCode + "}\r\n";
		dest = addRequiredImportList(dest);
		return dest;
	}

	@Override
	public String getUnifiedVersionTestCaseSourceCode(
			String currentTestCaseSourceCode, JUnitVersion version) {
		String dest = currentTestCaseSourceCode;
		ClassMeta classMeta = new ClassMetaExtractor(config)
				.extract(currentTestCaseSourceCode);
		if (version == JUnitVersion.version3) {
			dest = dest.replaceAll("@Test[\\s\r\n]*public void ",
					"public void test" + config.testMethodName.basicDelimiter);
			String[] splittedArray = config.testCaseClassNameToExtend
					.split("\\.");
			String testCaseName = splittedArray[splittedArray.length - 1];
			dest = dest.replaceFirst(classMeta.name + "\\s*\\{", classMeta.name
					+ " extends " + testCaseName + " {");
			dest = addRequiredImportList(dest);
		} else if (version == JUnitVersion.version4) {
			dest = dest.replaceAll("public void test"
					+ config.testMethodName.basicDelimiter,
					"@Test \r\n\tpublic void ");
			dest = dest.replaceFirst(classMeta.name
					+ "\\s+extends\\s+.+\\s*\\{", classMeta.name + " {");
			dest = addRequiredImportList(dest);
		}
		return dest;
	}

	String addRequiredImportList(String src) {

		String dest = src;
		String oneline = TrimFilterUtil.doAllFilters(src);

		StringBuilder importedListBuf = new StringBuilder();
		for (String imported : targetClassMeta.importedList) {
			String newOne = "import " + imported + ";";
			if (!oneline.matches(RegExp.Anything_ZeroOrMore_Min + newOne
					+ RegExp.Anything_ZeroOrMore_Min)) {
				importedListBuf.append(newOne);
				importedListBuf.append(StringValue.CarriageReturn);
				importedListBuf.append(StringValue.LineFeed);
			}
		}

		// JUnit
		if (config.junitVersion == JUnitVersion.version3) {
			appendIfNotExists(importedListBuf, oneline, "import "
					+ config.testCaseClassNameToExtend + ";");
		} else if (config.junitVersion == JUnitVersion.version4) {
			appendIfNotExists(importedListBuf, oneline,
					"import static org.junit.Assert.*;");
			appendIfNotExists(importedListBuf, oneline,
					"import org.junit.Test;");
		}
		// Mock object framework
		if (config.mockObjectFramework == MockObjectFramework.EasyMock) {
			appendIfNotExists(importedListBuf, oneline,
					"import org.easymock.classextension.EasyMock;");
			appendIfNotExists(importedListBuf, oneline,
					"import org.easymock.classextension.IMocksControl;");
		} else if (config.mockObjectFramework == MockObjectFramework.JMock2) {
			appendIfNotExists(importedListBuf, oneline,
					"import org.jmock.Mockery;");
			appendIfNotExists(importedListBuf, oneline,
					"import org.jmock.Expectations;");
			appendIfNotExists(importedListBuf, oneline,
					"import org.jmock.lib.legacy.ClassImposteriser;");
		} else if (config.mockObjectFramework == MockObjectFramework.JMockit) {
			appendIfNotExists(importedListBuf, oneline, "import mockit.Mocked;");
			appendIfNotExists(importedListBuf, oneline,
					"import mockit.Expectations;");
		} else if (config.mockObjectFramework == MockObjectFramework.Mockito) {
			appendIfNotExists(importedListBuf, oneline,
					"import static org.mockito.BDDMockito.*;");
		}
		if (importedListBuf.length() > 0) {
			Matcher matcher = RegExp.PatternObject.PackageDefArea_Group
					.matcher(src.replaceAll(RegExp.CRLF, StringValue.Space));
			if (matcher.find()) {
				String packageDef = matcher.group(1);
				String replacement = packageDef + StringValue.CarriageReturn
						+ StringValue.LineFeed + StringValue.CarriageReturn
						+ StringValue.LineFeed + importedListBuf.toString();
				dest = dest.replaceFirst(packageDef, replacement);
			} else {
				dest = importedListBuf.toString() + dest;
			}
		}
		return dest;

	}

	void appendIfNotExists(StringBuilder buf, String src, String importLine) {
		String oneline = src.replaceAll(RegExp.CRLF, StringValue.Space);
		if (!oneline.matches(RegExp.Anything_ZeroOrMore_Min
				+ importLine.replaceAll("\\s+", "\\\\s+")
				+ RegExp.Anything_ZeroOrMore_Min)) {
			buf.append(importLine);
			buf.append(StringValue.CarriageReturn);
			buf.append(StringValue.LineFeed);
		}
	}

}
