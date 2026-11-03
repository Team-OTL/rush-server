import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from './../src/app.module';
import { setupSwagger } from './../src/swagger';

describe('AppController (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('user');
    setupSwagger(app);
    await app.init();
  });

  it('/user/ping (GET)', () => {
    return request(app.getHttpServer()).get('/user/ping').expect(200);
  });

  it('/user/v3/api-docs (GET)', async () => {
    const response = await request(app.getHttpServer())
      .get('/user/v3/api-docs')
      .expect(200);
    const body = response.body as { openapi: string; servers?: unknown[] };
    expect(body.openapi).toMatch(/^3\./);
    expect(body.servers ?? []).toHaveLength(0);
  });
});
