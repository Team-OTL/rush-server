import { INestApplication } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';

/**
 * OpenAPI 문서를 /sp/v3/api-docs 에 냅니다. 게이트웨이 Swagger UI가 이 경로로 문서를 가져옵니다.
 * servers를 비워 두면 Swagger UI가 문서를 받아 온 게이트웨이 주소로 요청을 보냅니다.
 * Authorize에 넣은 JWT가 모든 요청에 붙도록 Bearer 인증을 전체 API에 적용합니다.
 */
export function setupSwagger(app: INestApplication): void {
  const config = new DocumentBuilder()
    .setTitle('rush-sp')
    .setVersion('1.0')
    .addBearerAuth()
    .addSecurityRequirements('bearer')
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('sp/docs', app, document, {
    jsonDocumentUrl: 'sp/v3/api-docs',
    yamlDocumentUrl: 'sp/v3/api-docs.yaml',
  });
}
