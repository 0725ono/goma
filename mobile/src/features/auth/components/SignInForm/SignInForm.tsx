import { Controller } from "react-hook-form";
import {
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

import { useSignInForm } from "@/features/auth/hooks/useSignInForm";

export function SignInForm() {
  const { control, errors, onSubmit } = useSignInForm();

  return (
    <View>
      <Controller
        control={control}
        name="email"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextInput
            style={styles.input}
            placeholder="メールアドレス"
            keyboardType="email-address"
            autoCapitalize="none"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
          />
        )}
      />
      {errors.email && <Text style={styles.error}>{errors.email.message}</Text>}

      <Controller
        control={control}
        name="password"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextInput
            style={styles.input}
            placeholder="パスワード"
            secureTextEntry
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
          />
        )}
      />
      {errors.password && (
        <Text style={styles.error}>{errors.password.message}</Text>
      )}

      <TouchableOpacity style={styles.button} onPress={onSubmit}>
        <Text style={styles.buttonText}>SignIn</Text>
      </TouchableOpacity>

      <View style={styles.ssoPlaceholder}>
        <Text style={styles.ssoText}>※将来ここにSSOボタンを配置</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  input: {
    backgroundColor: "#FFF",
    borderWidth: 1,
    borderColor: "#DDD",
    padding: 15,
    borderRadius: 8,
    marginBottom: 6,
    fontSize: 16,
  },
  error: {
    color: "#D32F2F",
    marginBottom: 9,
    marginLeft: 4,
    fontSize: 13,
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
