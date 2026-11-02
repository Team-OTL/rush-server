import { Controller, Get, Headers } from '@nestjs/common';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Get('ping')
  ping() {
    return this.appService.ping();
  }

  // 게이트웨이가 JWT 검증 후 넣어준 X-User-Id 헤더를 읽는 예시
  @Get('me')
  me(@Headers('x-user-id') userId?: string) {
    return { userId: userId ?? null };
  }
}
