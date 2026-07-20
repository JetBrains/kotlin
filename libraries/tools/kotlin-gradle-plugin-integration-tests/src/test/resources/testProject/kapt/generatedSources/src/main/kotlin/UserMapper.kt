import User.UserInfo
import org.mapstruct.Mapper

@Mapper
interface UserMapper {
    fun mapUserInfo(source: UserInfo): UserInfo2
}