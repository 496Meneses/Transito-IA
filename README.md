```markdown
# 🚦 Transito-IA — Backend (Spring Boot)

Backend desarrollado con **Spring Boot** para el proyecto Transito-IA. Este servicio expone APIs REST, gestiona persistencia, seguridad y la lógica necesaria para soportar el prototipo Asistente de Movilidad Inteligente.

El README sigue la misma estructura informativa que el frontend y conserva la sección "Desarrollado por" al final.

---

## 📋 Descripción

El backend se encarga de:
- Exponer endpoints REST para obtener y reportar siniestros.
- Ejecutar lógica predictiva / integración con modelos de IA (si aplica).
- Gestionar persistencia en base de datos (MySQL/H2).
- Documentación de API (OpenAPI / Swagger).

Está pensado para funcionar junto con el frontend React del proyecto y otros servicios (modelos ML, ingestión IoT, etc.).

---

## 🚀 Características principales

- ✅ Endpoints para consultar y reportar siniestros y alertas.
- ✅ Integración con una base de datos relacional (Postgres recomendado).
- ✅ Documentación automática con Springdoc OpenAPI / Swagger.
- ✅ Contenedorizable con Docker

---

## 🛠️ Tecnologías utilizadas

- Java 17+ (o la versión configurada en el proyecto)
- Spring Boot (Web, Data JPA, Security, Actuator, Validation)
- Spring Data JPA (Hibernate)
- Springdoc OpenAPI / Swagger
- MySQL / H2
- Maven
- Lombok
- Docker / Docker Compose

---

## 📦 Requisitos previos

- JDK 17+
- Git
- Maven 3.6+
- PostgreSQL (o usar H2 para desarrollo)
- (Opcional) Docker

---

## 📥 Instalación y configuración

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/496Meneses/Transito-IA.git
   cd Transito-IA
   ```

2. Configurar variables de entorno o editar `src/main/resources/application.properties` / `application.yml`:

   Ejemplo (application.properties):
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/transito_ia
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true

   server.port=8080

   # JWT u otros secretos
   app.jwt.secret=REPLACE_WITH_SECRET
   app.jwt.expiration-ms=3600000
   ```

   Variables de entorno recomendadas:
   - SPRING_DATASOURCE_URL
   - SPRING_DATASOURCE_USERNAME
   - SPRING_DATASOURCE_PASSWORD
   - APP_JWT_SECRET
   - SPRING_PROFILES_ACTIVE

---

## ▶️ Ejecutar en local

Si el proyecto usa Maven (mvnw incluido):
```bash
# Compilar y ejecutar
./mvnw clean package
./mvnw spring-boot:run

# o ejecutar el jar generado
java -jar target/transito-ia-0.0.1-SNAPSHOT.jar
```

Accede a la API en:
http://localhost:8080

---

## 🐳 Docker (opcional)

Dockerfile de ejemplo:
```dockerfile
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

docker-compose ejemplo (Postgres + app):
```yaml
version: "3.8"
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: transito_ia
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - db-data:/var/lib/postgresql/data

  backend:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/transito_ia
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      APP_JWT_SECRET: supersecreto
    ports:
      - "8080:8080"
    depends_on:
      - db

volumes:
  db-data:
```

---

## 🧩 Estructura sugerida del proyecto

(Adapta a la estructura real del repo)
```
transito-ia-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/transitoia/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       └── config/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
├── Dockerfile
├── docker-compose.yml
├── mvnw / gradlew
└── README.md
```

---

## 🗄️ Base de Datos

### Consola H2
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (vacío)

### Modelo de Datos

```sql
CREATE TABLE accidente (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha DATE,
    direccion VARCHAR(255),
    tipo VARCHAR(100)
);
```

## 📊 Dataset

El proyecto incluye un dataset con más de 199,000 registros de accidentes de tráfico en Bogotá que contiene:

- **Ubicación**: Coordenadas, dirección, localidad
- **Temporal**: Fecha y hora del accidente
- **Clasificación**: Tipo de accidente, gravedad
- **Identificación**: Códigos únicos, formularios

## 🧠 Funcionamiento de la IA

### 1. Procesamiento de Embeddings
- Los datos del dataset se convierten en descripciones textuales
- Se generan embeddings usando el modelo de OpenAI
- Los embeddings se almacenan para búsquedas de similitud

### 2. Búsqueda por Similitud
- Se calcula la similitud coseno entre embeddings
- Se retornan los TOP 10 resultados más similares
- Algoritmo optimizado con PriorityQueue

### 3. Generación de Alertas
- Se combinan datos históricos con prompts inteligentes
- GPT-3.5-turbo genera recomendaciones contextuales
- Respuestas humanizadas y específicas para Bogotá

## 🔧 Configuración Avanzada

### Personalizar Límites
```java
private static final int TOP_K = 10;           # Resultados de búsqueda
private static final int LIMITE_DATASET = 100; # Registros a procesar
```

---

## 🎯 Objetivo del proyecto

Fortalecer la infraestructura tecnológica del Ministerio de Transporte de Colombia a través de servicios backend robustos que soporten la toma de decisiones basada en IA, contribuyendo a la reducción de accidentes y a una movilidad más inteligente y segura.

---

## 👨‍💻 Desarrollado por

Proyecto académico de la **Maestría en Arquitectura de Software – Tendencias Emergentes**.  
Universidad Cooperativa de Colombia – 2025.
