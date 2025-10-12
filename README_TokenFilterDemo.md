# Demostración de Token Filters de Apache Lucene

Este proyecto demuestra el uso de diferentes token filters de Apache Lucene sobre un texto de ejemplo.

## Archivos incluidos

- `TokenFilterDemo.java`: Programa principal que demuestra los diferentes filtros
- `compile_and_run.sh`: Script para compilar y ejecutar automáticamente
- `README_TokenFilterDemo.md`: Este archivo de instrucciones

## Token Filters demostrados

1. **StandardFilter**: Normaliza tokens según reglas estándar
2. **LowerCaseFilter**: Convierte tokens a minúsculas
3. **StopFilter**: Elimina palabras vacías (stop words)
4. **SnowballFilter**: Aplica stemming (reducción a raíz)
5. **ShingleFilter**: Genera n-gramas de tokens

## Dependencias necesarias

El programa requiere los siguientes archivos JAR de Lucene:

- `lucene-core-10.3.1.jar` (ubicado en `lucene-10.3.1/modules/`)
- `lucene-analysis-common-10.3.1.jar` (ubicado en `lucene-10.3.1/modules/`)

## Cómo ejecutar

### Opción 1: Usar el script automático

```bash
./compile_and_run.sh
```

### Opción 2: Compilación y ejecución manual

#### En Linux/Mac:

```bash
# Compilar
javac -cp "lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo.java

# Ejecutar
java -cp ".:lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo
```

#### En Windows:

```cmd
# Compilar
javac -cp "lucene-10.3.1/modules/lucene-core-10.3.1.jar;lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo.java

# Ejecutar
java -cp ".;lucene-10.3.1/modules/lucene-core-10.3.1.jar;lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo
```

## Salida esperada

El programa mostrará:

1. **Tokenización básica**: Tokens sin procesar
2. **Con StandardFilter**: Tokens normalizados
3. **Con LowerCaseFilter**: Tokens en minúsculas
4. **Con StopFilter**: Tokens sin palabras vacías
5. **Con SnowballFilter**: Tokens con stemming aplicado
6. **Con ShingleFilter**: N-gramas de tokens
7. **Combinación completa**: Todos los filtros aplicados secuencialmente

## Estructura del proyecto

```
P2/
├── TokenFilterDemo.java          # Programa principal
├── compile_and_run.sh           # Script de compilación y ejecución
├── README_TokenFilterDemo.md    # Este archivo
└── lucene-10.3.1/              # Directorio de Lucene
    └── modules/
        ├── lucene-core-10.3.1.jar
        └── lucene-analysis-common-10.3.1.jar
```

## Notas importantes

- Asegúrese de que Java esté instalado en su sistema
- Los archivos JAR deben estar en las rutas especificadas
- En Windows, use punto y coma (;) en lugar de dos puntos (:) en el classpath
- El programa analiza un texto de ejemplo en inglés que incluye diferentes tipos de palabras para demostrar mejor el efecto de cada filtro
