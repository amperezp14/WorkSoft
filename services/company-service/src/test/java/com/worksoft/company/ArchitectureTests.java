package com.worksoft.company;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.worksoft.company", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {
    @ArchTest
    static final ArchRule domain_is_independent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..configuration..",
                    "org.springframework..", "jakarta..", "org.hibernate..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule application_uses_ports = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..configuration..", "org.springframework..",
                    "jakarta..", "org.hibernate..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule core_does_not_depend_on_bootstrap = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.worksoft.company.CompanyServiceApplication")
            .allowEmptyShould(false);
}
