import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  // 게이트웨이가 /user/** 를 그대로 넘겨주므로 prefix를 맞춥니다.
  app.setGlobalPrefix('user');
  await app.listen(process.env.PORT ?? 8085);
}
void bootstrap();
