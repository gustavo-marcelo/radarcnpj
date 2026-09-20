package ia.espalha.cnpj.importacao;

public record CnpjColumn(String name, String sqlType, ColumnTransform transform) {

	public CnpjColumn(String name, String sqlType) {
		this(name, sqlType, ColumnTransform.NONE);
	}
}