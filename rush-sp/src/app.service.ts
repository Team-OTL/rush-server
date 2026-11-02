import { Injectable } from '@nestjs/common';

@Injectable()
export class AppService {
  ping() {
    return { service: 'rush-sp', status: 'ok' };
  }
}
