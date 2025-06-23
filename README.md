(public\s+class\s+\w+Converter\s+implements\s+AttributeConverter<)(\w+)(>\s*\{\s*private\s+static\s+final\s+long\s+serialVersionUID\s*=\s*[^;]+;\s*@Override\s+public\s+)\2(\s+createTypKlassenInstanz\(String\s+value\)\s*\{\s*if\s*\(\s*value\s*!=\s*null\s*\)\s*\{\s*return\s+new\s+)\2(\(value\)\s*;\s*\}\s*return\s+new\s+)\2(\(\)\s*;\s*\})

${1}${2},String${3}String convertToDatabaseColumn(${2} attribute) {
    if (attribute == null) {
        return null;
    }
    return attribute.toString();
}

@Override
public ${2} convertToEntityAttribute(String dbData) {
    if (dbData != null) {
        return new ${2}(dbData);
    }
    return new ${2}();
}
