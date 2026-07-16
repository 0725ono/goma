import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
} from "react-native";
// router は使わないので削除し、_layout.tsx から useAuth を読み込みます
import { useAuth } from "../_layout";

export default function LoginScreen() {
  // Context から signIn 関数を取り出す
  const { signIn } = useAuth();

  const handleLogin = () => {
    // router.replace("/(private)"); は削除！
    // 代わりに Context の signIn() を実行して、状態を「ログイン済み（true）」にする
    signIn();
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>家族スケジュール</Text>
      <Text style={styles.subtitle}>SignIn</Text>

      <TextInput
        style={styles.input}
        placeholder="メールアドレス"
        keyboardType="email-address"
        autoCapitalize="none"
      />
      <TextInput
        style={styles.input}
        placeholder="パスワード"
        secureTextEntry
      />

      <TouchableOpacity style={styles.button} onPress={handleLogin}>
        <Text style={styles.buttonText}>SignIn</Text>
      </TouchableOpacity>

      <View style={styles.ssoPlaceholder}>
        <Text style={styles.ssoText}>※将来ここにSSOボタンを配置</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    justifyContent: "center",
    backgroundColor: "#F5F5F5",
  },
  title: {
    fontSize: 28,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 10,
    color: "#333",
  },
  subtitle: {
    fontSize: 16,
    textAlign: "center",
    marginBottom: 30,
    color: "#666",
  },
  input: {
    backgroundColor: "#FFF",
    borderWidth: 1,
    borderColor: "#DDD",
    padding: 15,
    borderRadius: 8,
    marginBottom: 15,
    fontSize: 16,
  },
  button: {
    backgroundColor: "#007AFF",
    padding: 15,
    borderRadius: 8,
    alignItems: "center",
    marginTop: 10,
  },
  buttonText: { color: "#FFF", fontSize: 16, fontWeight: "bold" },
  ssoPlaceholder: {
    marginTop: 30,
    alignItems: "center",
    padding: 15,
    borderWidth: 1,
    borderColor: "#CCC",
    borderStyle: "dashed",
    borderRadius: 8,
  },
  ssoText: { color: "#999" },
});
