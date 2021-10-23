package io.github.albertus82.extfix;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.IVersionProvider;

@Slf4j
public class VersionProvider implements IVersionProvider {

	@Override
	public String[] getVersion() {
		return new String[] { "${COMMAND-FULL-NAME} " + BuildInfo.getProperty("project.version") + " (" + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(getVersionTimestamp()) + ')' };
	}

	private static TemporalAccessor getVersionTimestamp() {
		try {
			return DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(BuildInfo.getProperty("version.timestamp"));
		}
		catch (final RuntimeException e) {
			log.debug("Invalid version timestamp, falling back to current datetime:", e);
			return ZonedDateTime.now();
		}
	}

}
