package com.grupob.futbolapi

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@AnalyzeClasses(packages = ["com.grupob.futbolapi"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    @ArchTest
    fun transactionalAnnotationShouldOnlyBeUsedOnServices(importedClasses: JavaClasses) {
        val rule: ArchRule = classes()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beAnnotatedWith(Service::class.java)
            .andShould().resideInAPackage("..services..")
            .because("Transactional annotations should only be used on the service layer.")

        rule.check(importedClasses)
    }
}
