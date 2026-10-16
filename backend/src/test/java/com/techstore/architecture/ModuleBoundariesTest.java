package com.techstore.architecture;

import com.techstore.TechStoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleBoundariesTest {

    @Test
    void verifiesModuleStructureAndDependencies() {
        ApplicationModules.of(TechStoreApplication.class).verify();
    }
}
