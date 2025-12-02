package com.grupob.futbolapi

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

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

    @ArchTest
    fun exceptionsClassesShouldEndWithException(importedClasses: JavaClasses) {
        classes().that().resideInAPackage("..exceptions..")
            .should().haveSimpleNameEndingWith("Exception").check(importedClasses)
    }

    @ArchTest
    fun persistanceClassesShouldEndWithRepo(importedClasses: JavaClasses) {
        classes().that().resideInAPackage("..repositories..")
            .should().haveSimpleNameEndingWith("Repository").check(importedClasses)
    }

    @ArchTest
    fun webserviceClassesShouldEndWithControllerOrWebService(importedClasses: JavaClasses) {
        classes().that().resideInAPackage("..webServices..")
            .should().haveSimpleNameEndingWith("Controller")
            .orShould().haveSimpleNameEndingWith("WebService")
            .check(importedClasses)
    }

    //@ArchTest
    //fun layeredArchitectureShouldBeRespected(importedClasses: JavaClasses) {
    //    layeredArchitecture()
    //        .consideringAllDependencies()
    //        .layer("Controller").definedBy("..webServices..")
    //        .layer("Service").definedBy("..services..")
    //        .layer("Persistence").definedBy("..repositories..")

    //        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
    //        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
    //        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
    //        .check(importedClasses)
    //}

    @ArchTest
    fun controllerClassesShouldHaveSpringControllerAnnotation(importedClasses: JavaClasses) {
        classes().that().resideInAPackage("..webServices..")
            .should().beAnnotatedWith(RestController::class.java)
            .check(importedClasses)
    }
}
