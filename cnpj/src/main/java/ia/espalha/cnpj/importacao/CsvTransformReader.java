package ia.espalha.cnpj.importacao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

final class CsvTransformReader extends Reader {

	private final BufferedReader source;
	private final CnpjTable table;
	private final StringBuilder pending = new StringBuilder(512);
	private boolean done;

	CsvTransformReader(BufferedReader source, CnpjTable table) {
		this.source = source;
		this.table = table;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		while (pending.length() == 0 && !done) {
			fillNext();
		}
		if (pending.length() == 0) {
			return -1;
		}
		int n = Math.min(len, pending.length());
		pending.getChars(0, n, cbuf, off);
		pending.delete(0, n);
		return n;
	}

	@Override
	public void close() throws IOException {
		source.close();
	}

	private void fillNext() throws IOException {
		String row = readLogicalRow();
		if (row == null) {
			done = true;
			return;
		}
		List<String> fields = split(row);
		List<CnpjColumn> cols = table.columns();
		StringBuilder line = new StringBuilder(256);
		boolean first = true;
		for (int i = 0; i < cols.size(); i++) {
			if (!first) {
				line.append(';');
			}
			first = false;
			String raw = i < fields.size() ? fields.get(i) : "";
			String value = transform(cols.get(i), raw);
			if (value.isEmpty()) {
				continue;
			}
			line.append('"').append(value.replace("\"", "\"\"")).append('"');
		}
		line.append('\n');
		pending.append(line);
	}

	private String readLogicalRow() throws IOException {
		String first = source.readLine();
		if (first == null) {
			return null;
		}
		if (hasBalancedQuotes(first)) {
			return first;
		}
		StringBuilder sb = new StringBuilder(first);
		String line;
		while ((line = source.readLine()) != null) {
			sb.append('\n').append(line);
			if (hasBalancedQuotes(sb.toString())) {
				break;
			}
		}
		return sb.toString();
	}

	private static boolean hasBalancedQuotes(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '"') {
				count++;
			}
		}
		return count % 2 == 0;
	}

	private static List<String> split(String row) {
		List<String> fields = new ArrayList<>(32);
		StringBuilder current = new StringBuilder(64);
		boolean inQuotes = false;
		for (int i = 0; i < row.length(); i++) {
			char c = row.charAt(i);
			if (inQuotes) {
				if (c == '"') {
					if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
						current.append('"');
						i++;
					} else {
						inQuotes = false;
					}
				} else {
					current.append(c);
				}
			} else {
				if (c == '"') {
					inQuotes = true;
				} else if (c == ';') {
					fields.add(current.toString());
					current.setLength(0);
				} else {
					current.append(c);
				}
			}
		}
		fields.add(current.toString());
		return fields;
	}

	private static String transform(CnpjColumn column, String raw) {
		if (raw.isEmpty()) {
			return raw;
		}
		return switch (column.transform()) {
			case DATE -> toIsoDate(raw);
			case DECIMAL_BR -> toDecimal(raw);
			case NONE -> raw.replace("\u0000", "");
		};
	}

	private static String toIsoDate(String raw) {
		if (raw.length() != 8) {
			return "";
		}
		for (int i = 0; i < 8; i++) {
			if (!Character.isDigit(raw.charAt(i))) {
				return "";
			}
		}
		String candidate = raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
		try {
			return LocalDate.parse(candidate).toString();
		} catch (DateTimeParseException e) {
			return "";
		}
	}

	private static String toDecimal(String raw) {
		String value = raw.replace(".", "").replace(',', '.');
		if (value.isEmpty() || !value.matches("-?\\d{1,16}(\\.\\d{1,2})?")) {
			return "";
		}
		return value;
	}
}