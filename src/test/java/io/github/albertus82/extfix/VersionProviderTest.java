package io.github.albertus82.extfix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionProviderTest {

	@Test
	void testVersionProvider() {
		final VersionProvider vp = new VersionProvider();
		Assertions.assertNotEquals(0, vp.getVersion().length);
	}

}
